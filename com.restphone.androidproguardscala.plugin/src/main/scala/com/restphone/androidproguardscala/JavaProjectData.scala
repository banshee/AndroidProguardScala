package com.restphone.androidproguardscala

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.ui.preferences.ScopedPreferenceStore
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.IPath
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.IClasspathEntry
import scala.PartialFunction._
import scalaz._
import Scalaz._
import org.eclipse.core.internal.runtime.Log
import org.eclipse.jdt.internal.core.ClasspathEntry

class JavaProjectData( project: IJavaProject ) {
  def getProject = project.getProject

  def classpathEntries: Array[ ClasspathEntryData ] =
    classpathData map { _.data }

  case class ClasspathData( entry: IClasspathEntry, data: ClasspathEntryData )

  private def classpathData =
    for {
      classpathentry <- project.getResolvedClasspath( true ) if ( ( classpathentry.getContentKind ^ IPackageFragmentRoot.K_SOURCE ) != 0 )
      prefname <- ClasspathEntryData.convertPathToPreferenceName( classpathentry.getPath.toOSString )
    } yield ClasspathData( classpathentry, ClasspathEntryData( prefname, classpathentry.getPath.toOSString ) )

  def outputDirectories = {
    val cpes = project.getResolvedClasspath( true ) filter { _.getEntryKind == IClasspathEntry.CPE_SOURCE } filter { _.getContentKind == IPackageFragmentRoot.K_SOURCE }
    val specificOutputLocations = cpes flatMap { c => Option( c.getOutputLocation ) }
    val optionalDirectories = ( project.getOutputLocation :: specificOutputLocations.toList ).toSet map convertPathToFilesystemPath
    optionalDirectories.flatten
  }

  val preferences = {
    val projectScope: IScopeContext = new ProjectScope( getProject )
    val store = new ScopedPreferenceStore( projectScope, "com.restphone.androidproguardscala" );
    for {
      ClasspathData( entry, data ) <- classpathData
      defaultValue = defaultValueForClasspathEntry( entry.getPath )
    } yield {
      store.setDefault( data.fieldName, defaultValue.asString )
    }
    store
  }

  def defaultValueForClasspathEntry( classpathEntryPath: IPath ): ClasspathEntryType = {
    val MatchesAndroidSdk = ".*android-sdk.*".r
    val AndroidSupport = """android-support-v\d+.jar""".r
    val Scalaz = """scalaz-.*jar""".r
    val Akka = """akka-.*jar""".r
    val ( a, b ) = ( classpathEntryPath.toOSString, classpathEntryPath.lastSegment )
    val result = ( a, b ) match {
      case ( _, ( "scala-library.jar" | "scala-actors.jar" | "scala-reflect.jar" ) ) => InputJar
      case ( _, Scalaz() ) => InputJar
      case ( _, Akka() ) => InputJar
      case ( _, "android.jar" ) => LibraryJar
      case ( _, MatchesAndroidSdk() ) => LibraryJar
      case ( _, AndroidSupport() ) => LibraryJar
      case _ => IgnoredJar
    }
    result
  }

  def inputJars = jarsOfType( InputJar )
  def libraryJars = jarsOfType( LibraryJar )

  private def jarsOfType( t: ClasspathEntryType ) = {
    classpathEntries filter { stateForClasspathEntryData( _ ) == t } toList
  }

  private def stateForClasspathEntryData( c: ClasspathEntryData ): ClasspathEntryType = {
    val pref = preferences.getString( c.fieldName )
    ClasspathEntryType.convertStringToClasspathEntryType( pref ) getOrElse ( IgnoredJar )
  }

  def convertPathToFilesystemPath( p: IPath ) = {
    for {
      root <- Option( ResourcesPlugin.getWorkspace().getRoot() )
      member <- Option( root.findMember( p ) )
      rawLocation <- Option( member.getRawLocation )
      osString <- Option( rawLocation.toOSString )
    } yield osString
  }
}

