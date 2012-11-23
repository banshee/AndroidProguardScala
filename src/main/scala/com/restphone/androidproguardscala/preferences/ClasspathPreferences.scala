package com.restphone.androidproguardscala.preferences;

import com.restphone.androidproguardscala._
import org.eclipse.jface.preference._
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.IWorkbench
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.resources.ProjectScope
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.ui.dialogs.PropertyPage
import org.eclipse.ui.IWorkbenchPropertyPage
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.preferences.IEclipsePreferences
import scalaz._
import Scalaz._
import org.eclipse.swt.widgets.Composite
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.ui.preferences.ScopedPreferenceStore

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
  var element: IAdaptable = null
  var storage: Option[IEclipsePreferences] = none
  var classpathItemEditors: List[ClasspathEntryFieldEditor] = List.empty

  def getElement: IAdaptable = element

  def setElement( e: IAdaptable ) = {
    element = e
    val projectScope: IScopeContext = new ProjectScope( element.asInstanceOf[IProject] )
    val projectNode = projectScope.getNode( "com.restphone.androidproguardscala.preferences" );
    val projectStore :ScopedPreferenceStore = new ScopedPreferenceStore(projectScope, "qualifier");

    storage = some( projectNode )
  }

  setDescription( "Fnord!" )

  case class ClasspathEntryFieldEditor( displayLabel: String,
                                        fieldName: String,
                                        value: ClasspathEntryType,
                                        container: Composite )
    extends RadioGroupFieldEditor( fieldName, displayLabel, 3, ClasspathEntryFieldEditor.choiceValues,
      container ) {
  }
  object ClasspathEntryFieldEditor {
    val choiceValues = Array( Array( "Input Jar", "inputjar" ), Array( "Output Jar", "outputjar" ), Array( "Ignore", "ignore" ) )
  }

  def createFieldEditorForClasspathItem( c: ClasspathEntryData, container: Composite ) = {
    ClasspathEntryFieldEditor( c.displayLabel, c.fieldName, c.value, container )
  }

  override def createFieldEditors(): Unit = {
    val xs = ClasspathEntryData.classpathEntries( JavaCore.create( element.asInstanceOf[IProject] ) ).toList
    classpathItemEditors = xs map { c => createFieldEditorForClasspathItem( c, getFieldEditorParent ) }
    classpathItemEditors foreach { addField( _ ) }
    classpathItemEditors foreach { f => f.setPreferenceStore(store) }
  }

  override def performOk = {
    storage foreach { _.flush }
    true
  }
}
