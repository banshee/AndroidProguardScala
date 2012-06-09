package com.restphone.androidproguardscala

import org.eclipse.core.resources.IProject
import java.util.ArrayList
import com.google.common.collect.Lists
import org.eclipse.core.runtime.IPath

object ProjectUtilities {
  def outputFolders(p : IProject) : ArrayList[IPath] = {
    val items = scala.tools.eclipse.ScalaProject(p).sourceOutputFolders.map { case (src, dest) => dest.getLocation }
    val jarl = new ArrayList[IPath]();
    items.foreach {x => jarl.add(x)}
    return jarl;
  }
  
}