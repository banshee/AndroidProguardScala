package com.restphone.androidproguardscala

import org.eclipse.core.resources.IProject
import java.util.ArrayList
import com.google.common.collect.Lists
import org.eclipse.core.runtime.IPath
import scala.collection.mutable
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IClasspathEntry

object ProjectUtilities {
//  def outputFolders(p: IProject): ArrayList[IPath] = {
//    val items = scala.tools.eclipse.ScalaProject(p).sourceOutputFolders.map { case (src, dest) => dest.getLocation }
//    val jarl = new ArrayList[IPath]();
//    items.foreach { x => jarl.add(x) }
//    return jarl;
//  }
//
  def outputFolders(javaProject:IJavaProject): Seq[IPath] = {
    val outputs = new mutable.ArrayBuffer[IPath](10)
    for (cpe <- javaProject.getResolvedClasspath(true) if cpe.getEntryKind == IClasspathEntry.CPE_SOURCE) {
      val cpeOutput = cpe.getOutputLocation
      val output = if (cpeOutput == null) javaProject.getOutputLocation else cpeOutput
      outputs += output
    }
    outputs.toSeq
  }

}