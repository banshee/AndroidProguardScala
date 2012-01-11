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
import org.eclipse.core.runtime.Path

class AndroidProguardScalaBuilder extends IncrementalProjectBuilder {
  override def build(kind: Int, args: java.util.Map[String, String], monitor: IProgressMonitor): Array[IProject] = {
    val a = mapAsScalaMap(args)
    com.restphone.JrubyEnvironmentSetup.addJrubyJarfile(pathForJarFile(classOf[org.jruby.Ruby]))
    loadClassIntoJRuby(classOf[org.objectweb.asm.Type])
    loadClassIntoJRuby(classOf[proguard.Initializer])
    
    com.restphone.JrubyEnvironmentSetup.addToLoadPath(relativeToRoot("jruby"))

    val outputJar = relativeToRoot("scala_compressed.jar")
    val cacheDirectory = relativeToRoot("proguard_cache")
    val cachedJar = relativeToRoot("proguard_cache/scala-library.CKSUM.jar")
    val proguardConfFile = relativeToRoot("proguard.conf")
    val jr = new com.restphone.ProguardCacheRuby
    jr.build_dependency_files_and_final_jar(outputFoldersPathsAsStrings, proguardConfFile, outputJar, cacheDirectory, cachedJar)
    Array.empty[IProject]
  }

  def relativeToRoot(path: String) = new java.io.File(rootDirectoryOfProject, path).toString

  def scalaProject = scala.tools.eclipse.ScalaProject(getProject)

  def outputFolders = scalaProject outputFolders

  def outputFoldersPathsAsStrings = outputFolders map { _.toString } toArray

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
    com.restphone.JrubyEnvironmentSetup.addToLoadPath(path)
  }
}

object AndroidProguardScalaBuilder {
  val BUILDER_ID = "AndroidProguardScala.androidProguardScala";
}
