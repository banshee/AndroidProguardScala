package com.restphone.androidproguardscala

import java.util.Iterator
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectDescription
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IObjectActionDelegate
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.jface.viewers.IStructuredSelection
import scala.collection.JavaConversions._
import org.eclipse.ui.commands.IElementUpdater
import org.eclipse.ui.menus.UIElement
import org.eclipse.jdt.core.JavaCore

class ToggleNatureAction extends IObjectActionDelegate with IElementUpdater {
  var selection: Option[ISelection] = None

  override def run(action: IAction): Unit = {
    selection collect {
      case s: IStructuredSelection =>
        s.iterator collect {
          case p: IProject => p
          case a: IAdaptable => a.getAdapter(classOf[IProject]).asInstanceOf[IProject]
        } foreach { toggleNature(_) }
    }
  }

  def selectionChanged(action: IAction, s: ISelection) = {
    selection = Some(s)
  }

  override def updateElement(arg0: UIElement, arg1: java.util.Map[_, _]) = println("arg0: " + arg0 + " " + arg1)

  def setActivePart(a: IAction, target: IWorkbenchPart) = {}

  def toggleNature(project: IProject) = {
    if (project.isOpen) {
      val description = project.getDescription
          val natures = description.getNatureIds
          val newNatures = natures find isApsNatureName match {
          case Some(_) => natures filterNot isApsNatureName
          case None => natures ++ Array(AndroidProguardScalaNature.NATURE_ID)
      }
      description.setNatureIds(newNatures)
      project.setDescription(description, null)
      project.touch(null)
    }
  }

  def moveScalaClasspathContainersEarlyInTheClasspath(project: IProject): Unit = {
    // Move the scala classpath containers to be early in the classpath.  They have to be before the Android
    // classpath container or the scala presentation compiler crashes.

    // Yes, this is an ugly hack.

    val (scalaCpes, nonScalaCpes) = javaProject(project).getRawClasspath partition { cpe => cpe.getPath.toString.contains("SCALA") }
    val newClasspath = scalaCpes ++ nonScalaCpes
    javaProject(project).setRawClasspath(newClasspath, null)
  }

  def javaProject(p: IProject) = JavaCore.create(p)

  def isApsNatureName(s: String) = s == AndroidProguardScalaNature.NATURE_ID
}