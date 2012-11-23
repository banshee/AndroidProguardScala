package com.restphone.androidproguardscala

import org.eclipse.ui.preferences.ScopedPreferenceStore
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope

class JartenderProjectPreferences(storage: ScopedPreferenceStore) {
	def asScopedPreferenceStore = storage
	
	def stateForClasspathEntryData(c: ClasspathEntryData): ClasspathEntryType = {
	  ClasspathEntryType.convertStringToClasspathEntryType(storage.getString(c.fieldName)) getOrElse IgnoredJar
	}
}

object JartenderProjectPreferences {
  def apply( p: IProject ) = {
    val projectScope: IScopeContext = new ProjectScope( p )
    val projectStore: ScopedPreferenceStore = new ScopedPreferenceStore( projectScope, "com.restphone.androidproguardscala" );
    new JartenderProjectPreferences(projectStore)
  }
}