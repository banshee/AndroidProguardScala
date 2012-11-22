package com.restphone.androidproguardscala

import scala.beans.BeanProperty

import org.eclipse.core.resources.ICommand
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IProjectNature

class AndroidProguardScalaNature extends IProjectNature {
  @BeanProperty var project: IProject = null

  def existingCommands = project.getDescription.getBuildSpec
  def builderNameMatches( x: ICommand ) = x.getBuilderName == AndroidProguardScalaBuilder.BUILDER_ID
  def existingBuilder = existingCommands find builderNameMatches

  override def configure =
    existingBuilder getOrElse {
      val description = project.getDescription
      val command = description.newCommand
      command.setBuilderName( AndroidProguardScalaBuilder.BUILDER_ID )
      val builders = existingCommands ++ Array( command )
      description.setBuildSpec( builders )
      project.setDescription( description, null )
    }

  override def deconfigure = {
    existingBuilder foreach {
      _ =>
        val otherBuilders = existingCommands filterNot builderNameMatches
        val description = project.getDescription
        description.setBuildSpec( otherBuilders )
        project.setDescription( description, null )
    }
  }
}

object AndroidProguardScalaNature {
  val NATURE_ID = "com.restphone.androidproguardscala.Nature"
}
