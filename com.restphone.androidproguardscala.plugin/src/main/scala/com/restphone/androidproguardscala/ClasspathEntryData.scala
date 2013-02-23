package com.restphone.androidproguardscala

import java.io.File

import com.restphone.jartender.RichFile

import scalaz.Scalaz._

case class ClasspathEntryData( fieldName: String, fullPath: String ) 

object ClasspathEntryData {
  def convertPathToPreferenceName( pathAsString: String ) = {
    val files = RichFile.splitFile( new File( pathAsString ) )
    val elementNames = files map {_.getName}
    elementNames.reverse match {
      case a :: b :: t => some( f"jarfile_${a}_${b}" )
      case a :: t => some( f"jarfile_x_${a}" )
      case _ => None
    }
  }
}

sealed abstract class ClasspathEntryType {
  val asString: String
}
case object InputJar extends ClasspathEntryType {
  val asString = ClasspathEntryType.INPUTJAR
}
case object LibraryJar extends ClasspathEntryType {
  val asString = ClasspathEntryType.LIBRARYJAR
}
case object IgnoredJar extends ClasspathEntryType {
  val asString = ClasspathEntryType.IGNORE
}

object ClasspathEntryType {
  def convertStringToClasspathEntryType( s: String ) = s match {
    case INPUTJAR => some( InputJar )
    case LIBRARYJAR => some( LibraryJar )
    case IGNORE => some( IgnoredJar )
    case _ => none[ClasspathEntryType]
  }

  val INPUTJAR = "inputjar"
  val LIBRARYJAR = "libraryjar"
  val IGNORE = "ignore"
}
