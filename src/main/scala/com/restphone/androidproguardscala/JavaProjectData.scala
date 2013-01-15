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

class JavaProjectData( project: IJavaProject ) {
  def getProject = project.getProject

  def classpathEntries: Array[ClasspathEntryData] =
    for {
      classpathentry <- project.getResolvedClasspath( true ) if ( ( classpathentry.getContentKind ^ IPackageFragmentRoot.K_SOURCE ) != 0 )
      prefname <- ClasspathEntryData.convertPathToPreferenceName( classpathentry.getPath.toOSString )
    } yield ClasspathEntryData( prefname, classpathentry.getPath.toOSString )

  def outputDirectories = {
    val cpes = project.getResolvedClasspath( true ) filter { _.getEntryKind == IClasspathEntry.CPE_SOURCE } filter { _.getContentKind == IPackageFragmentRoot.K_SOURCE }
    val specificOutputLocations = cpes flatMap { c => Option( c.getOutputLocation ) }
    val optionalDirectories = ( project.getOutputLocation :: specificOutputLocations.toList ).toSet map convertPathToFilesystemPath
    optionalDirectories.flatten
  }

  lazy val preferences = {
    val projectScope: IScopeContext = new ProjectScope( getProject )
    new ScopedPreferenceStore( projectScope, "com.restphone.androidproguardscala" );
  }

  def inputJars = jarsOfType( InputJar )
  def libraryJars = jarsOfType( LibraryJar )

  private def jarsOfType( t: ClasspathEntryType ) = {
    classpathEntries filter { stateForClasspathEntryData( _ ) == t } toList
  }

  private def stateForClasspathEntryData( c: ClasspathEntryData ): ClasspathEntryType = {
    val pref =  preferences.getString( c.fieldName )
    ClasspathEntryType.convertStringToClasspathEntryType(pref ) getOrElse (IgnoredJar)
  }

  def convertPathToFilesystemPath( p: IPath ) = {
    for {
      root <- Option(ResourcesPlugin.getWorkspace().getRoot())
      member <- Option(root.findMember(p))
      rawLocation <- Option(member.getRawLocation)
      osString <- Option(rawLocation.toOSString)
    } yield osString
  }
}

