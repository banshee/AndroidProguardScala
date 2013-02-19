package com.restphone.androidproguardscala

import org.eclipse.jface.preference.IPreferenceStore

class Activator extends org.eclipse.ui.plugin.AbstractUIPlugin {
  override def getPreferenceStore: IPreferenceStore = super.getPreferenceStore
}

object Activator {
  lazy val defaultActivator = new Activator
}