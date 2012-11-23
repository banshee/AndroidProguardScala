package com.restphone.androidproguardscala

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IPackageFragmentRoot

object JavaProjectData {
  def classpathEntries( p: IJavaProject ) =
    for {
      classpathentry <- p.getResolvedClasspath( true ) if ( ( classpathentry.getContentKind ^ IPackageFragmentRoot.K_SOURCE ) != 0 )
      prefname <- ClasspathEntryData.convertPathToPreferenceName( classpathentry.getPath.toOSString )
    } yield ClasspathEntryData( prefname, classpathentry.getPath.toOSString )
}