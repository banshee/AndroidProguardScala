package com.restphone.androidproguardscala

import com.restphone._
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IProgressMonitor
import scala.collection.JavaConversions._
import org.eclipse.core.resources.ResourcesPlugin
import scala.tools.eclipse.ScalaProject
import org.eclipse.jdt.core.JavaCore
import java.net.URI
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Platform
import org.osgi.framework.BundleContext
import java.util.ResourceBundle
import java.io.File

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val a = mapAsScalaMap(args)
    val proguardConfFile = relativeToRoot("proguard.conf")
    val outputJar = relativeToRoot("scala_compressed.jar")
    val cacheDirectory = relativeToRoot("proguard_cache")
    val cachedJar = relativeToRoot("proguard_cache/scala-library.CKSUM.jar")
    rubyCacheController.build_dependency_files_and_final_jar(outputFoldersPathsAsStrings, proguardConfFile, outputJar, cacheDirectory, cachedJar)
    Array.empty[IProject]
  }

  lazy val rubyCacheController = {
    JrubyEnvironmentSetup.addJrubyJarfile(pathForJarFile(classOf[org.jruby.Ruby]))

    loadClassIntoJRuby(classOf[org.objectweb.asm.Type])
    loadClassIntoJRuby(classOf[proguard.Initializer])
    JrubyEnvironmentSetup.addToLoadPath(pluginDirectory + "src/main/jruby")
    new ProguardCacheRuby
  }

  def relativeToRoot(path: String) = new java.io.File(rootDirectoryOfProject, path).toString

  def scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def outputFolders = scalaProject outputFolders

  def outputFoldersPathsAsStrings = outputFolders map { _.toString } map {new java.io.File(rootDirectoryOfWorkspace, _).toString} toArray

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
    println("bundle is " + bundle)
    val path = new Path("/");
    val fileURL = FileLocator.find(bundle, path, null);
    val f = FileLocator.toFileURL(fileURL)
    val configFile = new File(fileURL.getPath())
    println("fsefr: " + configFile + f.getFile)
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
