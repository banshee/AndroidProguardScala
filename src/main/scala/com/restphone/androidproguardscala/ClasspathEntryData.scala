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
import org.eclipse.jdt.core.IPackageFragmentRoot

case class ClasspathEntryData( fieldName: String, displayLabel: String ) {
}
object ClasspathEntryData {
  // This is hacky, but all we're looking for are the last two elements of the path,
  // so it should work.
  private def splitPath( s: String ) = s.replace( '\\', '/' ).split( '/' ).toList

  def convertPathToPreferenceName( pathAsString: String ) = {
    splitPath( pathAsString ).reverse match {
      case a :: b :: t => some( f"jarfile_${a}_${b}" )
      case a :: t => some( f"jarfile_x_${a}" )
      case _ => None
    }
  }
}

sealed abstract class ClasspathEntryType
case object InputJar extends ClasspathEntryType
case object OutputJar extends ClasspathEntryType
case object IgnoredJar extends ClasspathEntryType

object ClasspathEntryType {
  def convertStringToClasspathEntryType(s: String) = s match {
    case INPUTJAR => some(InputJar)
    case OUTPUTJAR => some(OutputJar)
    case IGNORE => some(IgnoredJar)
    case _ => none[ClasspathEntryType]
  }

  val INPUTJAR = "inputjar"
  val OUTPUTJAR = "outputjar"
  val IGNORE = "ignore"
}
