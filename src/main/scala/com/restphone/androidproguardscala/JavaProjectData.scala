package com.restphone.androidproguardscala

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragmentRoot
import org.eclipse.ui.preferences.ScopedPreferenceStore
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.runtime.IPath
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.IClasspathEntry

import scalaz._
import Scalaz._

class JavaProjectData( project: IJavaProject ) {
  def getProject = project.getProject

  def classpathEntries: Array[ClasspathEntryData] =
    for {
      classpathentry <- project.getResolvedClasspath( true ) if ( ( classpathentry.getContentKind ^ IPackageFragmentRoot.K_SOURCE ) != 0 )
      prefname <- ClasspathEntryData.convertPathToPreferenceName( classpathentry.getPath.toOSString )
    } yield ClasspathEntryData( prefname, classpathentry.getPath.toOSString )

  def outputDirectories = {
    val cpes = project.getResolvedClasspath( true ) filter { _.getEntryKind == IClasspathEntry.CPE_SOURCE } filter { _.getContentKind == IPackageFragmentRoot.K_SOURCE }
    val specificOutputLocations = cpes flatMap { c => Option(c.getOutputLocation) }
    (project.getOutputLocation :: specificOutputLocations.toList).toSet map convertPathToFilesystemPath 
  }

  lazy val preferences = {
    val projectScope: IScopeContext = new ProjectScope( project.getProject )
    new ScopedPreferenceStore( projectScope, "com.restphone.androidproguardscala" );
  }

  def inputJars = jarsOfType( InputJar )
  def libraryJars = jarsOfType( LibraryJar )

  private def jarsOfType( t: ClasspathEntryType ) = {
    classpathEntries filter { c => stateForClasspathEntryData( c ) == t }
  }

  private def stateForClasspathEntryData( c: ClasspathEntryData ): ClasspathEntryType = {
    ClasspathEntryType.convertStringToClasspathEntryType( preferences.getString( c.fieldName ) ) getOrElse IgnoredJar
  }

  def convertPathToFilesystemPath( p: IPath ) = {
    val root = ResourcesPlugin.getWorkspace().getRoot();
    root.findMember(p).getRawLocation.toOSString
  }
}
