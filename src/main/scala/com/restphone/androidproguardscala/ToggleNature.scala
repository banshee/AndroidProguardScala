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

class ToggleNatureAction extends IObjectActionDelegate {
  var selection: Option[ISelection] = None

  override def run(action: IAction): Unit = {
    selection collect {
      case s: IStructuredSelection =>
        val items = s.iterator.asInstanceOf[Iterator[AnyRef]].toList collect {
          case p: IProject => p
          case a: IAdaptable => a.getAdapter(classOf[IProject]).asInstanceOf[IProject]
        }
        items foreach toggleNature
    }
  }

  def selectionChanged(action: IAction, s: ISelection) = {
    selection = Some(s)
  }

  def setActivePart(a: IAction, target: IWorkbenchPart) = {}

  def toggleNature(project: IProject) = {
    val description = project.getDescription
    val natures = description.getNatureIds
    val newNatures = natures find isApsNatureName match {
      case Some(_) => natures filterNot isApsNatureName
      case None => natures ++ Array(ApsNature.NATURE_ID)
    }
    description.setNatureIds(newNatures)
  }

  def isApsNatureName(s: String) = s == ApsNature.NATURE_ID
}