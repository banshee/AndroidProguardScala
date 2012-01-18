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

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichFile._

  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val scalaArgs = mapAsScalaMap(args.asInstanceOf[java.util.Map[String, String]])

    val cacheDirectory = rootDirectoryOfProject / "proguard_cache"

    val proguardDefaults = slurp(pathToFileRelativeToPluginBundle(new Path("proguard_cache/proguard_defaults.conf")))
    val proguardProcessedConfFile = cacheDirectory / "proguard_postprocessed.conf"
    val proguardAdditionsFile = cacheDirectory / "proguard_additions.conf"

    val cachedJar = cacheDirectory / "scala-library.CKSUM.jar"

    val outputJar = rootDirectoryOfProject / "lib" / "scala_lib_after_proguard.jar"

    val parameters = Map(
      "cacheDir" -> cacheDirectory.getAbsolutePath,
      "classFiles" -> (outputFoldersFiles map objToString toArray),
      "proguardDefaults" -> proguardDefaults,
      "proguardAdditionsFile" -> proguardAdditionsFile.getAbsolutePath,
      "proguardProcessedConfFile" -> proguardProcessedConfFile.getAbsolutePath,
      "cachedJar" -> cachedJar.getAbsolutePath,
      "outputJar" -> outputJar.getAbsolutePath,
      "scalaLibraryJar" -> pathToScalaLibraryJar)

    println("-------------------------------------------")
    println("params are " + parameters)

    rubyCacheController.build_dependency_files_and_final_jar(parameters)

    Array.empty
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
    println("jrubylibdir is " + jrubyLibDir)
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
    println("getting class " + c)
    c.getProtectionDomain.getCodeSource.getLocation.getPath
  }

  def loadClassIntoJRuby[T](c: Class[T]) = {
    val p = pathForJarFileContainingClass(c)
    loadJarIntoJRuby(p)
  }

  def loadJarIntoJRuby(path: String) = {
    JrubyEnvironmentSetup.addJarToLoadPathAndRequire(path)
  }

  def pathToFileRelativeToPluginBundle(p: Path) = {
    val bundle = Platform.getBundle("com.restphone.androidproguardscala");
    val entry = bundle.getEntry(p.toString)
//    val fileURL = FileLocator.find(bundle, p, null);
    val f = FileLocator.toFileURL(entry)
    println("relfile:" + f)
    new File(f.getFile)
  }

  def pluginDirectory = pathToFileRelativeToPluginBundle(new Path("/"))
}

object AndroidProguardScalaBuilder {
  val BUILDER_ID = "com.restphone.androidproguardscala.Builder";
}

class Activator extends org.eclipse.ui.plugin.AbstractUIPlugin {
  override def startup = {
    println("starwerup")
    super.startup();
  }

  override def start(context: BundleContext) {
    println("contextaseer is " + context)
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

//-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
//                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
//
//# Change com.restphone to your own package
//-keep public class com.restphone.* {
//    *;
//}
//
//-keep public class scala.App
//-keep public class scala.DelayedInit
//-keep public class scala.ScalaObject
//-keep public class scala.Function0, scala.Function1, scala.collection.mutable.ListBuffer

class RichFile(f: File) {
  def /(that: String) = new File(f, that)
}
object RichFile {
  implicit def toRichFile(f: File): RichFile = new RichFile(f)
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
