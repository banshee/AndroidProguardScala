package com.restphone.androidproguardscala.preferences;

import java.io.File
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.preference.FieldEditorPreferencePage
import org.eclipse.jface.preference.RadioGroupFieldEditor
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.IWorkbenchPropertyPage
import com.restphone.androidproguardscala.ClasspathEntryData
import com.restphone.androidproguardscala.ClasspathEntryType.IGNORE
import com.restphone.androidproguardscala.ClasspathEntryType.INPUTJAR
import com.restphone.androidproguardscala.ClasspathEntryType.LIBRARYJAR
import com.restphone.androidproguardscala.IgnoredJar
import com.restphone.androidproguardscala.InputJar
import com.restphone.androidproguardscala.JavaProjectData
import com.restphone.jartender.RichFile
import com.restphone.androidproguardscala.ClasspathEntryType
import com.restphone.androidproguardscala.LibraryJar
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

class ClasspathPreferences
  extends FieldEditorPreferencePage( FieldEditorPreferencePage.GRID )
  with IWorkbenchPropertyPage {
  var projectData: JavaProjectData = null
  var classpathItemEditors: List[ ClasspathEntryFieldEditor ] = List.empty

  def setElement( e: IAdaptable ) = {
    projectData = new JavaProjectData( JavaCore.create( e.asInstanceOf[ IProject ] ) )
    setPreferenceStore( projectData.preferences )
  }

  override def getElement = projectData.getProject

  setDescription( "Choose the jar files that will be included in the shrunken final jar.  Input jars are included, library jars are passed to Proguard, and ignored files are ignored.\r\r\r" )

  case class ClasspathEntryFieldEditor( displayLabel: String,
                                        fieldName: String,
                                        fullPath: String,
                                        container: Composite )
    extends RadioGroupFieldEditor( fieldName, displayLabel, 3, ClasspathEntryFieldEditor.choiceValues,
      container ) {
  }
  object ClasspathEntryFieldEditor {
    val choiceValues = Array( Array( "Input Jar", INPUTJAR ), Array( "Library Jar", LIBRARYJAR ), Array( "Ignore", IGNORE ) )
  }

  def createFieldEditorForClasspathItem( c: ClasspathEntryData, container: Composite ) = {
    ClasspathEntryFieldEditor( c.fullPath, c.fieldName, c.fullPath, container )
  }

//  def defaultValueForClasspathEntryFieldEditor( c: ClasspathEntryFieldEditor ): ClasspathEntryType = {
//    val MatchesAndroidSdk = ".*android-sdk.*".r
//    val AndroidSupport = """android-support-v\d+.jar""".r
//    val Scalaz = """scalaz-.*jar""".r
//    val Akka = """akka-.*jar""".r
//    val s = new File( c.fullPath ).getName + " " + c.toString
//    println(s)
//    ( c.fullPath, new File( c.fullPath ).getName ) match {
//      case ( _, ( "scala-swing.jar" ) ) => IgnoredJar
//      case ( _, ( "scala-library.jar" | "scala-actors.jar" | "scala-reflect.jar" ) ) => InputJar
//      case ( _, Scalaz() ) => InputJar
//      case ( _, Akka() ) => InputJar
//      case ( _, "android.jar" ) => LibraryJar
//      case ( _, MatchesAndroidSdk() ) => LibraryJar
//      case ( _, AndroidSupport() ) => LibraryJar
//      case _ => IgnoredJar
//    }
//  }
//
  override def createFieldEditors(): Unit = {
    val xs = projectData.classpathEntries.toList
    classpathItemEditors = xs map { c => createFieldEditorForClasspathItem( c, getFieldEditorParent ) }
    classpathItemEditors foreach { f =>
      val defaultValue = projectData.defaultValueForClasspathEntry(new Path(f.fullPath))
      projectData.preferences.setDefault( f.fieldName, defaultValue.asString )
      addField( f )
      f.setPreferenceStore( projectData.preferences )
      f.setPage( this );
      f.load();
    }
  }
}
