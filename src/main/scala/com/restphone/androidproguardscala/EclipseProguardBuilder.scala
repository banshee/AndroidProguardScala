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

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  import RichFile.toRichFile

  override def build(kind: Int, args: java.util.Map[_, _], monitor: IProgressMonitor): Array[IProject] = {
    val scalaArgs = mapAsScalaMap(args.asInstanceOf[java.util.Map[String, String]])
    val cacheDirectory = relativeToRoot("proguard_cache")
    val proguardConfFile = cacheDirectory / "proguard.conf"
    val proguardProcessedConfFile = cacheDirectory / "proguard_postprocessed.conf"
    val outputJar = cacheDirectory / "scala_lib_after_proguard.jar"
    val cachedJar = cacheDirectory / "scala-library.CKSUM.jar"
    val outputFolders = outputFoldersPaths map objToString
    rubyCacheController.build_dependency_files_and_final_jar(outputFolders, proguardConfFile, proguardProcessedConfFile, outputJar, cacheDirectory, cachedJar)
    Array.empty
  }

  lazy val rubyCacheController = {
    JrubyEnvironmentSetup.addJrubyJarfile(pathForJarFile(classOf[org.jruby.Ruby]))

    loadClassIntoJRuby(classOf[org.objectweb.asm.Type])
    loadClassIntoJRuby(classOf[proguard.Initializer])

    JrubyEnvironmentSetup.addToLoadPath(pluginDirectory + "src/main/jruby")

    new ProguardCacheRuby
  }

  def relativeToRoot(path: String) = new java.io.File(rootDirectoryOfProject, path)

  def scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def outputFolders = scalaProject outputFolders

  def objToString[T](x: T) = x.toString

  def outputFoldersPaths = outputFolders map { x => new File(rootDirectoryOfWorkspace, x.toString) } toArray

  def rootDirectoryOfProject = {
    getProject.getLocation.toOSString
  }

  def rootDirectoryOfWorkspace = {
    ResourcesPlugin.getWorkspace.getRoot.getLocation.toOSString
  }

  def pathForJarFile[T](c: Class[T]) = {
    c.getProtectionDomain.getCodeSource.getLocation.getPath
  }

  def loadClassIntoJRuby[T](c: Class[T]) = {
    val p = pathForJarFile(c)
    loadJarIntoJRuby(p)
  }

  def loadJarIntoJRuby(path: String) = {
    JrubyEnvironmentSetup.addJarToLoadPathAndRequire(path)
  }

  def pluginDirectory = {
    val bundle = Platform.getBundle("com.restphone.androidproguardscala");
    val path = new Path("/");
    val fileURL = FileLocator.find(bundle, path, null);
    val f = FileLocator.toFileURL(fileURL)
    f.getFile
  }
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
}
