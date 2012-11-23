package com.restphone.androidproguardscala

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.core.runtime.IStatus
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IPath
import java.io.File
import scalaz._
import Scalaz._

case class ClasspathEntryData( fieldName: String, displayLabel: String ) {
}
object ClasspathEntryData {
  private def splitPath( s: String ) = s.replace( '\\', '/' ).split( '/' ).toList

  def convertPathToPreferenceName( p: IPath ) = {
    splitPath( p.toOSString ).reverse match {
      case a :: b :: t => some( f"jarfile_${a}_${b}" )
      case a :: t => some( f"jarfile_x_${a}" )
      case _ => None
    }
  }

  def classpathEntries( p: IJavaProject ) =
    for {
      classpathentry <- p.getResolvedClasspath( true )
      prefname <- convertPathToPreferenceName( classpathentry.getPath )
    } yield ClasspathEntryData( prefname, classpathentry.getPath.toOSString )
}

sealed abstract class ClasspathEntryType
case object InputJar extends ClasspathEntryType
case object OutputJar extends ClasspathEntryType
case object IgnoredJar extends ClasspathEntryType

