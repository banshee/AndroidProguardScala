package com.restphone.androidproguardscala

import scala.PartialFunction._
import java.io.File
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.seqAsJavaList
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.core.resources.IResourceDelta
import org.jruby.Ruby
import org.objectweb.asm.Type
import org.osgi.framework.BundleContext
import proguard.Initializer
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.resources.IResourceDelta
import com.restphone.androidproguardscala.jruby._

trait ProvidesLogging {
  def logMsg(msg: String)
  def logError(msg: String)
}

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichFile._

  def pathIsBuildArtifact(p: IPath) = p.lastSegment.indexOf("proguard_") == 0
  val streamContainsOnlyBuildArtifacts: PartialFunction[Stream[IPath], Boolean] = {
    case h #:: Stream.Empty if (pathIsBuildArtifact(h)) => true
  }
  def inverse[T](fn: PartialFunction[T, Boolean]) = fn andThen {!_}
  val streamContainsNonBuildArtifacts = inverse(streamContainsOnlyBuildArtifacts)

  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val affected_paths = getDelta(getProject) match {
      case x: IResourceDelta => x.getAffectedChildren map { _.getFullPath }
      case null => Array.empty[IPath]
    }
    val buildRequired = cond(affected_paths.toStream)(streamContainsNonBuildArtifacts)

    logMsg("build is required: " + buildRequired)

    if (buildRequired) {
      val scalaArgs = mapAsScalaMap(args.asInstanceOf[java.util.Map[String, String]])

      val proguardDefaults = slurp(pathToFileRelativeToPluginBundle(new Path("proguard_cache_conf/proguard_defaults.conf")))

      Seq(cacheDirectory, confDirectory, libDirectory) foreach ensureDirExists

      val proguardProcessedConfFile = confDirectory / "proguard_postprocessed.conf"
      val proguardAdditionsFile = confDirectory / "proguard_additions.conf"

      val cachedJar = cacheDirectory / "scala-library.CKSUM.jar"

      val outputJar = rootDirectoryOfProject / "lib" / "scala_library.min.jar"

      import scala.collection.JavaConversions.asJavaMap

      val parameters = {
        val fileParameters = {
          val javaFileParameters: Map[String, File] = Map(
            "cacheDir" -> cacheDirectory,
            "confDir" -> confDirectory,
            "workspaceDir" -> rootDirectoryOfWorkspace,
            "projectDir" -> rootDirectoryOfProject,
            "proguardAdditionsFile" -> proguardAdditionsFile,
            "proguardProcessedConfFile" -> proguardProcessedConfFile,
            "cachedJar" -> cachedJar,
            "outputJar" -> outputJar,
            "scalaLibraryJar" -> scalaLibraryJar,
            "androidLibraryJar" -> pathToAndroidJar.toFile)
          javaFileParameters mapValues toRubyFile
        }

        val otherParameters = Map(
          "classFiles" -> (outputFoldersFiles map toRubyFile toArray),
          "proguardDefaults" -> proguardDefaults,
          "logger" -> logger())

        fileParameters ++ otherParameters
      }

      logMsg("Build parameters are: " + parameters)

      if (buildRequired) {
        // Using asJavaMap because JRuby has magic that adds many Ruby Hash methods to 
        // Java Map objects.
        rubyCacheController.build_dependency_files_and_final_jar(asJavaMap(parameters))
      }
    }

    Array.empty
  }

  override def clean(monitor: IProgressMonitor): Unit = rubyCacheController.clean_cache(toRubyFile(cacheDirectory))

  lazy val rootDirectoryOfProject = ipathToFile(getProject.getLocation)
  lazy val cacheDirectory = rootDirectoryOfProject / "proguard_cache"
  lazy val confDirectory = rootDirectoryOfProject / "proguard_cache_conf"
  lazy val libDirectory = rootDirectoryOfProject / "lib"
  lazy val scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def outputFolders: Seq[IPath] = scalaProject outputFolders
  def outputFoldersFiles = outputFolders map ipathToFile map { f => rootDirectoryOfWorkspace / f.toString }

  def toRubyFile(f: File) = f.toString.replace('\\', '/')

  def logger() = new ProvidesLogging {
    def logMsg(msg: String) = AndroidProguardScalaBuilder.this.logMsg(msg)
    def logError(msg: String) = AndroidProguardScalaBuilder.this.logMsg(msg, IStatus.ERROR)
  }

  val lastSegmentIsString = (s: String) => (p: IPath) => p.lastSegment.equals(s)
  val lastSegmentIsScalaLibrary = lastSegmentIsString("scala-library.jar")
  val lastSegmentIsAndroidLibrary = lastSegmentIsString("android.jar")
  val fileExists = (p: IPath) => p.toFile.exists

  def scalaLibraryJar = {
    val entry = getResolvedClasspathEntries filter lastSegmentIsScalaLibrary find fileExists
    entry map { f => new java.io.File(f.toString) } getOrElse null
  }

  def pathToAndroidJar = {
    val entry = getResolvedClasspathEntries filter lastSegmentIsAndroidLibrary find fileExists
    entry getOrElse null
  }

  def getResolvedClasspathEntries() = {
    val p = JavaCore.create(getProject)
    p.getResolvedClasspath(false) map { _.getPath }
  }

  lazy val rubyCacheController = {
    JrubyEnvironmentSetup.addJrubyJarfile(pathForJarFileContainingClass(classOf[org.jruby.Ruby]))

    loadClassIntoJRuby(classOf[org.objectweb.asm.Type])
    loadClassIntoJRuby(classOf[proguard.Initializer])
    loadClassIntoJRuby(classOf[List[String]])

    val jrubyLibDir = pluginDirectory / "src/main/jruby"
    JrubyEnvironmentSetup.addToLoadPath(jrubyLibDir.toString)

    new ProguardCacheRuby
  }

  def objToString[T](x: T) = x.toString

  def rootDirectoryOfWorkspace = {
    ipathToFile(ResourcesPlugin.getWorkspace.getRoot.getLocation)
  }

  def pathForJarFileContainingClass[T](c: Class[T]) = {
    c.getProtectionDomain.getCodeSource.getLocation.getPath
  }

  def loadClassIntoJRuby[T](c: Class[T]) = {
    val p = pathForJarFileContainingClass(c)
    loadJarIntoJRuby(p)
  }

  def loadJarIntoJRuby(path: String) = {
    JrubyEnvironmentSetup.addJarToLoadPathAndRequire(path)
  }

  val bundle = Platform.getBundle("com.restphone.androidproguardscala");

  def pathToFileRelativeToPluginBundle(p: IPath) = {
    val entry = bundle.getEntry(p.toString)
    val f = FileLocator.toFileURL(entry)
    new File(f.getFile)
  }

  def pluginDirectory = pathToFileRelativeToPluginBundle(new Path("/"))

  def logMsg(msg: String, status: Integer = IStatus.OK) = {
    val log = Platform.getLog(bundle);
    val s = new Status(status, pluginId, msg)
    log.log(s)
  }

  val pluginId = "com.restphone.androidproguardscala"
}

object AndroidProguardScalaBuilder {
  val BUILDER_ID = "com.restphone.androidproguardscala.Builder";
}

class Activator extends org.eclipse.ui.plugin.AbstractUIPlugin {
  override def startup = {
    super.startup();
  }

  override def start(context: BundleContext) {
    super.start(context);
  }
}

object Proguarder {
  def pathToProguardClassPathEntry(isOutput: Boolean)(p: String) = {
    new proguard.ClassPathEntry(new File(p), isOutput)
  }
  val pathToProguardClassPathOutputEntry = pathToProguardClassPathEntry(true)(_)
  val pathToProguardClassPathInputEntry = pathToProguardClassPathEntry(false)(_)

  def confForCache(scalaLib: String, inputDirs: Seq[String], outputJar: String, libJars: Seq[String]) = {
    val c = new proguard.Configuration

    val cp = new proguard.ClassPath

    cp.add(pathToProguardClassPathInputEntry(scalaLib + "(!META-INF/MANIFEST.MF,!library.properties)"))

    cp.add(pathToProguardClassPathOutputEntry(outputJar))

    def appendClasspathEntry(x: String) = cp.add(pathToProguardClassPathInputEntry(x))
    inputDirs foreach appendClasspathEntry
    libJars foreach appendClasspathEntry

    c.programJars = cp

    c.obfuscate = false
    c.warn = List.empty[String]
    c.optimize = false
    c.skipNonPublicLibraryClasses = false
    c.skipNonPublicLibraryClassMembers = false
    c.keepAttributes = List("Exceptions", "InnerClasses", "Signature", "Deprecated", "SourceFile", "LineNumberTable", "*Annotation*", "EnclosingMethod")
    c
  }
}

class RichFile(f: File) {
  def /(that: String) = new File(f, that)
}

object RichFile {
  implicit def toRichFile(f: File): RichFile = new RichFile(f)
  implicit def fileToPath(f: File): IPath = Path.fromOSString(f.toString)
  implicit def pathToFile(p: IPath): File = p.toFile
  def toFilenameAsString(f: File) = f.toString
  def stringToFile(f: String) = new File(f)
  def ipathToFile(p: IPath) = p.toFile
  def slurp(f: File) = {
    val s = scala.io.Source.fromFile(f)
    val result = s.getLines.mkString("\n")
    s.close()
    result
  }
  def ensureDirExists(f: File) =
    if (!f.exists) f.mkdir
}
