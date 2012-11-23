package com.restphone.androidproguardscala

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.core.runtime.IStatus
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status

case class ClasspathEntryData( fieldName: String, displayLabel: String, value: ClasspathEntryType ) {
}
object ClasspathEntryData {
  def classpathEntries( p: IJavaProject ) = {
    def classpathEntriesRecursive( acc: List[ClasspathEntryData], cpe: IClasspathEntry ) = {

    }
    p.getResolvedClasspath( true ) foreach { entry =>
      println(entry.toString)
    }
    p.getResolvedClasspath(true) map {x => ClasspathEntryData(x.getPath.toString, x.getPath.toString, InputJar)}
  }
}

sealed abstract class ClasspathEntryType
case object InputJar extends ClasspathEntryType
case object OutputJar extends ClasspathEntryType
case object IgnoredJar extends ClasspathEntryType

