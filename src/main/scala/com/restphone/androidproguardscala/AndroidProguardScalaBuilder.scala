package com.restphone.androidproguardscala

import java.io.File

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IWorkspaceRoot
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
import org.osgi.framework.BundleContext

import com.restphone.androidproguardscala.RichPath.toRichPath

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichPath._
  import RichFile._

  def buildArtifactsRequireRebuild(xs: Stream[IPath]): Boolean = {
    def pathIsBuildArtifact(p: IPath) = p.lastSegment.startsWith("proguard_")
    xs match {
      case h #:: Stream.Empty if (pathIsBuildArtifact(h)) => false
      case h #:: t if (pathIsBuildArtifact(h)) => buildArtifactsRequireRebuild(t)
      case _ => true
    }
  }

  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val buildRequired = {
      val affected_paths = getDelta(getProject) match {
        case x: IResourceDelta => x.getAffectedChildren map { _.getFullPath }
        case null => Array.empty[IPath]
      }
      buildArtifactsRequireRebuild(affected_paths.toStream)
    }

    if (buildRequired) {
      val proguardDefaults = {
        val pathToDefaultsFile = pluginDirectory.get / "proguard_cache_conf" / "proguard_defaults.conf"
        RichFile.slurp(pathToDefaultsFile.toFile)
      }

      Seq(cacheDir, confDir, libDirectory) foreach RichPath.ensureDirExists

      val proguardProcessedConfFile = confDir / "proguard_postprocessed.conf"
      val proguardAdditionsFile = confDir / "proguard_additions.conf"

      val cachedJar = cacheDir / "scala-library.CKSUM.jar"

      val outputJar = rootDirectoryOfProject / "libs" / AndroidProguardScalaBuilder.minifiedScalaLibraryName

      logMsg("output folders are " + existingOutputFolders)

      // classpath entry paths can be relative or absolute.  Absolute paths are usually
      // external libraries.
      //
      // _WARNING_: The Eclipse idea of an "absolute" path has nothing to do with what most people think
      // of as an absolute path.  In the world of Eclipse, an absolute path starts with a slash and
      // contains the name of the project as the first element.
      //
      // IT HAS NOTHING TO DO WITH A PATH TO AN OPERATING SYSTEM FILENAME STARTING WITH /.
      //
      // Also, to be even more annoying, Eclipse will occasionally return operating sytem paths that start
      // with a slash.
      //
      // Moral: NEVER trust an IPath.  Having just an IPath is utterly useless.

      val pathsToClasspathEntries = for {
        rawClasspathEntry <- javaProject.getRawClasspath if isCpeLibrary(rawClasspathEntry)
        relativePath <- NotNull(rawClasspathEntry.getPath, "getPath failed for " + rawClasspathEntry)
        libraryName <- NotNull(relativePath.lastSegment)
        member = getWorkspaceRoot.findMember(relativePath)
      } yield {
        logMsg("**** paths are " + relativePath + " " + relativePath.getClass)
        logMsg("paths are " + libraryName)
        logMsg("cpe is " + rawClasspathEntry)
        logMsg("member is " + member)
        val result = member match {
          case x: IResource => (convertResourceToFilesystemLocation(x), libraryName)
          case _ => (relativePath, libraryName)
        }
        logMsg("result is " + result._1 + " @@ " + result._2)
        result
      }

      val libraryLocations = pathsToClasspathEntries collect { case (path, jarname) if !isMinifiedLibraryName(jarname) => path }

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
        classFiles = (existingOutputFolders map convertIPathToString).toArray,
        libraryJars = (libraryLocations ++ List(pathToAndroidJar) map convertIPathToString),
        proguardDefaults = proguardDefaults,
        logger = logger)

      ((rubyCacheController.build_proguard_dependency_files _) andThen
        rubyCacheController.run_proguard andThen
        rubyCacheController.install_proguard_output)(params)

      logMsg("relativePathsAndLibraryNames is: " + (pathsToClasspathEntries collect { case (a, b) => a.toString + ", " + b.toString } mkString ", "))

      // ensureMinifiedLibraryIsOnClasspath
      pathsToClasspathEntries find { case (_, libraryName) => isMinifiedLibraryName(libraryName) } match {
        case None =>
          val newEntry = JavaCore.newLibraryEntry(outputJar, null, null)
          val newClasspath = javaProject.getRawClasspath ++ Iterable(newEntry)
          javaProject.setRawClasspath(newClasspath, monitor)
          logMsg("Added minified scala jar %s to classpath".format(outputJar))
        case Some(_) =>
      }

      Iterable(outputJar, confDir, cacheDir) foreach tellEclipsePathNeedsToBeRefreshed
    }

    Array.empty
  }

  def tellEclipsePathNeedsToBeRefreshed(p: IPath) = {
    getProject.getFile(p).refreshLocal(IResource.DEPTH_INFINITE, null)
  }

  lazy val rubyCacheController = ProguardCacheBuilder.buildCacheController(pluginDirectory.toString)

  override def clean(monitor: IProgressMonitor): Unit = rubyCacheController.clean_cache(cacheDir.toString)

  def rootDirectoryOfProject = convertResourceToFilesystemLocation(getProject)
  def cacheDir = rootDirectoryOfProject / "proguard_cache"
  def confDir = rootDirectoryOfProject / "proguard_cache_conf"
  def libDirectory = rootDirectoryOfProject / "libs"
  //  def scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def isCpeLibrary(x: IClasspathEntry) = x.getEntryKind == IClasspathEntry.CPE_LIBRARY
  def isMinifiedLibraryName(s: String) = s == AndroidProguardScalaBuilder.minifiedScalaLibraryName

  def convertResourceToFilesystemLocation(resource: IResource) = new Path(resource.getLocationURI.getPath)

  def existingOutputFolders = {
    // The IDE may have decided that some paths are the destination for class files without actually
    // creating those directories.  Only reporting ones that exist already.
    val outputFoldersAsIPaths = ProjectUtilities.outputFolders(getProject)
    import scala.collection.JavaConversions._
    asList(outputFoldersAsIPaths) filter fileExists toSet
  }

  def logger() = new ProvidesLogging {
    def logMsg(msg: String) = AndroidProguardScalaBuilder.this.logMsg(msg)
    def logError(msg: String) = AndroidProguardScalaBuilder.this.logMsg(msg, IStatus.ERROR)
  }

  private val lastSegmentIsString = (s: String) => (p: IPath) => p.lastSegment.equals(s)
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

  def objToString[T](x: T) = x.toString

  def getWorkspaceRoot: IWorkspaceRoot = ResourcesPlugin.getWorkspace.getRoot
  def rootDirectoryOfWorkspace: IPath = getWorkspaceRoot.getLocation

  val platformBundle = Platform.getBundle("com.restphone.androidproguardscala");

  // This seems like a hack, but it's apparently the right thing to do to find the plugin
  // directory.
  def pluginDirectory =
    for {
      u <- NotNull(FileLocator.find(platformBundle, new Path("/"), null), "cannot find directory for bundle")
      filenameUrl <- NotNull(Platform.resolve(u), "Platform.resolve must not return null")
      f = new Path(filenameUrl.getFile)
    } yield f

  def logMsg(msg: String, status: Integer = IStatus.INFO) = {
    val log = Platform.getLog(platformBundle);
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

class RichPath(p: IPath) {
  def /(that: String) = p.append(that)
}

object RichPath {
  implicit def toRichPath(p: IPath): RichPath = new RichPath(p)
  def ensureDirExists(p: IPath) = RichFile.ensureDirExists(p.toFile)
}
