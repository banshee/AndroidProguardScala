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
import javax.management.RuntimeErrorException

trait ProvidesLogging {
  def logMsg(msg: String)
  def logError(msg: String)
}

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichFile._

  def pathIsBuildArtifact(p: IPath) = p.lastSegment.indexOf("proguard_") == 0
  
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
    require(p != null, "must pass an IPath")
    val result = for {
      u <- NotNull(FileLocator.find(bundle, p, null), "cannot find path " + p)
      filenameUrl <- NotNull(Platform.resolve(u), "Platform.resolve must not return null")
      f = new File(filenameUrl.getFile)
    } yield f
    result.get
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

object NotNull {
  def apply[T](x: T, msg: String = "must not be null") = {
    val result = Option(x)
    if (result.isDefined) result
    else {
      throw new RuntimeException(msg)
    }
  }
}

import java.io.File

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
