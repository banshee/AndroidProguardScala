package com.restphone.androidproguardscala

import java.io.File
import scala.collection.JavaConversions.mapAsScalaMap
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore
import org.jruby.Ruby
import org.objectweb.asm.Type
import org.osgi.framework.BundleContext
import proguard.Initializer


class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichPath._
  import RichFile._

  def pathIsBuildArtifact(p: IPath) = p.lastSegment.startsWith("proguard_")

  def buildArtifactsRequireRebuild(xs: Stream[IPath]): Boolean = xs match {
    case h #:: Stream.Empty if (pathIsBuildArtifact(h)) => false
    case h #:: t if (pathIsBuildArtifact(h)) => buildArtifactsRequireRebuild(t)
    case _ => true
  }

  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val affected_paths = getDelta(getProject) match {
      case x: IResourceDelta => x.getAffectedChildren map { _.getFullPath }
      case null => Array.empty[IPath]
    }
    val buildRequired = buildArtifactsRequireRebuild(affected_paths.toStream)

    logMsg("build is required: " + buildRequired + " for artifacts " + affected_paths.mkString(", "))

    if (buildRequired) {
      val scalaArgs = mapAsScalaMap(args.asInstanceOf[java.util.Map[String, String]])

      val pathToDefaultsFile = pluginDirectory / "proguard_cache_conf" / "proguard_defaults.conf"
      val proguardDefaults = RichFile.slurp(pathToDefaultsFile.toFile)

      Seq(cacheDir, confDir, libDirectory) foreach RichPath.ensureDirExists

      val proguardProcessedConfFile = confDir / "proguard_postprocessed.conf"
      val proguardAdditionsFile = confDir / "proguard_additions.conf"

      val cachedJar = cacheDir / "scala-library.CKSUM.jar"

      val outputJar = rootDirectoryOfProject / "lib" / AndroidProguardScalaBuilder.minifiedScalaLibraryName

      import scala.collection.JavaConversions.asJavaMap

      logMsg("output folders are " + existingOutputFolders)

      def isCpeLibrary(x: IClasspathEntry) = x.getEntryKind == IClasspathEntry.CPE_LIBRARY
      def isMinifiedLibraryName(s: String) = s == AndroidProguardScalaBuilder.minifiedScalaLibraryName

      val processedClasspathEntries = for {
        rawClasspathEntry <- javaProject.getRawClasspath if isCpeLibrary(rawClasspathEntry)
        relativePath <- NotNull(rawClasspathEntry.getPath, "getPath failed for " + rawClasspathEntry)
        libraryName <- NotNull(relativePath.lastSegment)
      } yield (relativePath, libraryName)

      val libraryLocations: Array[IPath] = for {
        (relativePath, libraryName) <- processedClasspathEntries if !isMinifiedLibraryName(libraryName)
        member <- NotNull(getWorkspaceRoot.findMember(relativePath), "findMember failed for " + relativePath)
        locationWithExistingJar <- NotNull(member.getLocation, "getLocation failed for " + member) if fileExists(locationWithExistingJar)
      } yield locationWithExistingJar

      // Using asJavaMap because JRuby has magic that adds many Ruby Hash methods to 
      // Java Map objects.
      monitor.beginTask("Computing dependencies and running Proguard", 2)
      monitor.worked(1)

      implicit def convertIPathToString(p: IPath): String = p.toString

      val params = new ProguardCacheParameters(
        cacheDir = cacheDir,
        confDir = confDir,
        workspaceDir = rootDirectoryOfWorkspace,
        projectDir = rootDirectoryOfProject,
        proguardAdditionsFile = proguardAdditionsFile,
        proguardProcessedConfFile = proguardProcessedConfFile,
        cachedJar = cachedJar,
        outputJar = outputJar,
        scalaLibraryJar = scalaLibraryJar.toString,
        androidLibraryJar = pathToAndroidJar,
        classFiles = (existingOutputFolders map convertIPathToString).toArray,
        extraLibs = libraryLocations map convertIPathToString,
        proguardDefaults = proguardDefaults,
        logger = logger)

      executeSequenceOfProguardEvents(params)

      val classpathEntryForMinifedLibrary = processedClasspathEntries find { case (_, libraryName) => isMinifiedLibraryName(libraryName) }
      if (classpathEntryForMinifedLibrary.isEmpty) {
        val newEntry = JavaCore.newLibraryEntry(outputJar, null, null)
        val newClasspath = javaProject.getRawClasspath ++ Iterable(newEntry)
        javaProject.setRawClasspath(newClasspath, monitor)
        logMsg("Added minified scala jar %s to classpath".format(outputJar))
      }

      Iterable(outputJar, confDir, cacheDir) foreach tellEclipsePathNeedsToBeRefreshed
    }

    Array.empty
  }

  val executeSequenceOfProguardEvents = ((rubyCacheController.build_proguard_dependency_files _) andThen
    rubyCacheController.run_proguard andThen
    rubyCacheController.install_proguard_output)

  def tellEclipsePathNeedsToBeRefreshed(p: IPath) = {
    getProject.getFile(p).refreshLocal(IResource.DEPTH_INFINITE, null)
  }

  lazy val rubyCacheController = ProguardCacheBuilder.buildCacheController(pluginDirectory.toString)

  def projectContainsMinifiedOutput = {
    val entry = getResolvedClasspathEntries filter lastSegmentIsScalaLibrary find fileExists
    entry map { f => new java.io.File(f.toString) } getOrElse null
  }

  override def clean(monitor: IProgressMonitor): Unit = rubyCacheController.clean_cache(cacheDir.toString)

  lazy val rootDirectoryOfProject = getProject.getLocation
  lazy val cacheDir = rootDirectoryOfProject / "proguard_cache"
  lazy val confDir = rootDirectoryOfProject / "proguard_cache_conf"
  lazy val libDirectory = rootDirectoryOfProject / "lib"
  lazy val scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def existingOutputFolders = {
    // The IDE may have decided that some paths are the destination for class files without actually
    // creating those directories.  Only reporting ones that exist already.
    val outputFoldersAsIPaths = scalaProject.sourceOutputFolders.map { case (src, dest) => dest.getLocation }
    outputFoldersAsIPaths filter fileExists toSet
  }

  def logger() = new ProvidesLogging {
    def logMsg(msg: String) = AndroidProguardScalaBuilder.this.logMsg(msg)
    def logError(msg: String) = AndroidProguardScalaBuilder.this.logMsg(msg, IStatus.ERROR)
  }

  val lastSegmentIsString = (s: String) => (p: IPath) => p.lastSegment.equals(s)
  val lastSegmentIsScalaLibrary = lastSegmentIsString("scala-library.jar")
  val lastSegmentIsAndroidLibrary = lastSegmentIsString("android.jar")
  val fileExists = (p: IPath) => p.toFile.exists

  def scalaLibraryJar: File = {
    val entry = getResolvedClasspathEntries filter lastSegmentIsScalaLibrary find fileExists
    entry map { f => new java.io.File(f.toString) } getOrElse null
  }

  def pathToAndroidJar: IPath = {
    val entry = getResolvedClasspathEntries filter lastSegmentIsAndroidLibrary find fileExists
    if (entry.isDefined) entry.get
    else throw new RuntimeException("cannot find android library in " + getResolvedClasspathEntries)
  }

  lazy val javaProject = JavaCore.create(getProject)

  def getResolvedClasspathEntries() = {
    javaProject.getResolvedClasspath(false) map { _.getPath }
  }

  def getRawClasspathEntries = {
  }

  def objToString[T](x: T) = x.toString

  def getWorkspaceRoot = ResourcesPlugin.getWorkspace.getRoot
  def rootDirectoryOfWorkspace = {
    getWorkspaceRoot.getLocation
  }

  val bundle = Platform.getBundle("com.restphone.androidproguardscala");

  def pluginDirectory = {
    val result = for {
      u <- NotNull(FileLocator.find(bundle, new Path("/"), null), "cannot find directory for bundle")
      filenameUrl <- NotNull(Platform.resolve(u), "Platform.resolve must not return null")
      f = new Path(filenameUrl.getFile)
    } yield f
    result.get
  }

  def logMsg(msg: String, status: Integer = IStatus.OK) = {
    val log = Platform.getLog(bundle);
    val s = new Status(status, pluginId, msg)
    log.log(s)
  }

  val pluginId = "com.restphone.androidproguardscala"
}

object AndroidProguardScalaBuilder {
  val BUILDER_ID = "com.restphone.androidproguardscala.Builder";
  val minifiedScalaLibraryName = "scala_library.min.jar"
}

class Activator extends org.eclipse.ui.plugin.AbstractUIPlugin {
  override def startup = {
    super.startup();
  }

  override def start(context: BundleContext) {
    super.start(context);
  }
}

object NotNull {
  def apply[T](x: T, msg: String = "must not be null"): Option[T] = {
    val result = Option(x)

    if (result.isDefined) result
    else throw new RuntimeException(msg)
  }
}

class RichFile(f: File) {
  def /(that: String) = new File(f, that)
}

object RichFile {
  def slurp(f: File) = {
    val s = scala.io.Source.fromFile(f)
    val result = s.getLines.mkString("\n")
    s.close()
    result
  }
  def ensureDirExists(f: File) =
    if (!f.exists) f.mkdir
}

class RichPath(p: IPath) {
  def /(that: String) = p.append(that)
}

object RichPath {
  implicit def toRichPath(p: IPath): RichPath = new RichPath(p)
  implicit def convertFileToPath(f: java.io.File): IPath = Path.fromOSString(f.toString)
  implicit def convertUrlToPath(u: java.net.URL) = {
    val x = Platform.resolve(u)
    new Path(x.getFile)
  }
  def ensureDirExists(p: IPath) = RichFile.ensureDirExists(p.toFile)
}
