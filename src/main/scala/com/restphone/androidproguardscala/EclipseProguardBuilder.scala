package com.restphone.androidproguardscala

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
import org.jruby.Ruby
import org.objectweb.asm.Type
import org.osgi.framework.BundleContext
import proguard.Initializer
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus

trait ProvidesLogging {
  def logMsg(msg: String)
  def logError(msg: String)
}

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichFile._

  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val scalaArgs = mapAsScalaMap(args.asInstanceOf[java.util.Map[String, String]])

    val proguardDefaults = slurp(pathToFileRelativeToPluginBundle(new Path("proguard_cache_conf/proguard_defaults.conf")))

    val cacheDirectory = rootDirectoryOfProject / "proguard_cache"
    val confDirectory = rootDirectoryOfProject / "proguard_cache_conf"
    val libDirectory = rootDirectoryOfProject / "lib"

    cacheDirectory.mkdir
    confDirectory.mkdir
    libDirectory.mkdir

    val proguardProcessedConfFile = confDirectory / "proguard_postprocessed.conf"
    val proguardAdditionsFile = confDirectory / "proguard_additions.conf"

    val cachedJar = cacheDirectory / "scala-library.CKSUM.jar"

    val outputJar = rootDirectoryOfProject / "lib" / "scala_library_android.jar"

    val parameters = Map(
      "cacheDir" -> cacheDirectory.getAbsolutePath,
      "classFiles" -> (outputFoldersFiles map objToString toArray),
      "proguardDefaults" -> proguardDefaults,
      "proguardAdditionsFile" -> proguardAdditionsFile.getAbsolutePath,
      "proguardProcessedConfFile" -> proguardProcessedConfFile.getAbsolutePath,
      "cachedJar" -> cachedJar.getAbsolutePath,
      "outputJar" -> outputJar.getAbsolutePath,
      "scalaLibraryJar" -> pathToScalaLibraryJar,
      "logger" -> logger())

    rubyCacheController.build_dependency_files_and_final_jar(parameters)

    Array.empty
  }

  val outerThis = this
  
  def logger() = new ProvidesLogging {
    def logMsg(msg: String) = outerThis.logMsg(msg)
    def logError(msg: String) = outerThis.logMsg(msg, IStatus.ERROR)
  }

  def pathToScalaLibraryJar = {
    val lastSegmentIsScalaLibrary = (p: IPath) => p.lastSegment.equals("scala-library.jar")
    val fileExists = (p: IPath) => p.toFile.exists

    val p = JavaCore.create(getProject)
    val paths = p.getResolvedClasspath(false) map { _.getPath }
    val entry = paths filter lastSegmentIsScalaLibrary find fileExists

    entry.get
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

  def relativeToRoot(newItem: String) = rootDirectoryOfProject / newItem

  def scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def objToString[T](x: T) = x.toString

  def outputFolders = scalaProject outputFolders
  def outputFoldersFiles = outputFolders map ipathToFile map { f => rootDirectoryOfWorkspace / f.toString }

  def rootDirectoryOfProject = ipathToFile(getProject.getLocation)

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
  def toFilenameAsString(f: File) = f.toString
  def stringToFile(f: String) = new File(f)
  def ipathToFile(p: IPath) = p.toFile
  def slurp(f: File) = {
    val s = scala.io.Source.fromFile(f)
    val result = s.getLines.mkString("\n")
    s.close()
    result
  }
}
