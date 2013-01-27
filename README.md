OVERVIEW

AndroidProguardScala is an Eclipse plugin that speeds up the development process when you're using Scala on Android.

Scala + Android requires a Proguard run on every build.  That's slow.  The plugin watches code changes
and avoids running Proguard if no significant changes have happened.

The plugin is intended for development-time only.  You should continue to use Proguard with your own configuration
for release builds.

NOTE:  Your project must have Scala nature or the plugin will fail to run.

# USING THE PLUGIN

1.  Install the Scala IDE from http://download.scala-ide.org/nightly-update-master-trunk.  
There's a problem with M3 and Android (it won't always recompile
Scala binaries), so you'll want a nightly.

1.  Point Eclipse to:

https://androidproguardscala.s3.amazonaws.com/UpdateSiteForAndroidProguardScala

and install.

2.  Open an existing Android project, or create a new one.

2.  Add Scala nature to the project by right-clicking the project name.

2.  Right-click on your Android project and choose "Add AndroidProguardScala Nature."

3.  Open the properties for your project (right-click on the project name in the navigator, select properties from the menu)
and choose "AndroidProguardScala Properties".  Choose which jars in the classpath will be included in the minified jar.  By
default, the standard scala libraries will be included.

4.  After a build, notice that the directories 'proguard_cache' and 'proguard_cache_conf' are created.

proguard_cache contains the cached libraries and the files used to figure out which cached library to use (including whether or
not a new library needs to be generated.

proguard_cache_conf contains the generated proguard configuration files.  You should be able to use 
proguard_postprocessesd.conf as a config file outside of this tool.

# Release Notes

## v45

* Improved build process to avoid issues with categorization

## v44

* Fixed https://github.com/banshee/AndroidProguardScala/issues/22.  Defaults are now set correctly.  Before, defaults weren't set
until you visited the project settings page.

* 

# BUILDING THE PLUGIN ITSELF

* Get a copy of the Eclipse PDE - Plugin Development Environment version.

  As of 29 June 2010, that's found at:

    http://www.eclipse.org/downloads/index_project.php
