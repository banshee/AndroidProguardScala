package com.restphone.androidproguardscala

import scala.beans.BeanProperty

import org.eclipse.core.resources.ICommand
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectNature

class ApsNature extends IProjectNature {
  @BeanProperty var project: IProject = null

  def desc = project.getDescription
  def existingCommands = desc.getBuildSpec
  def builderNameMatches(x: ICommand) = x.getBuilderName == AndroidProguardScalaBuilder.BUILDER_ID
  def existingBuilder = existingCommands find builderNameMatches

  override def configure = {
    existingBuilder getOrElse {
      val command = desc.newCommand
      command.setBuilderName(AndroidProguardScalaBuilder.BUILDER_ID)
      desc.setBuildSpec(existingCommands ++ Array(command))
      project.setDescription(desc, null)
    }
  }

  override def deconfigure = {
    existingBuilder foreach { _ =>
      val otherBuilders = existingCommands filterNot builderNameMatches
      desc.setBuildSpec(otherBuilders)
      project.setDescription(desc, null)
    }
  }
}

object ApsNature {
  val NATURE_ID = "com.restphone.androidproguardscala.Nature"
}
