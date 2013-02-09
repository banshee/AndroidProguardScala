# OVERVIEW

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

1.  Point Eclipse to the update site at <code>https://androidproguardscala.s3.amazonaws.com/UpdateSiteForAndroidProguardScala</code> and install.
Note that this isn't a link you can visit in a browser.
Eclipse knows about the layout of files underneath that link, but you'll just get a 404 for the top level.

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
proguard_postprocessed.conf as a config file outside of this tool.

# Troubleshooting

* If you get a scala-library.min.jar, but it's empty, make sure you have a resonable set of libraries configured in the 
plugin properties.  Right click on your project and select AndroidProguardScala properties.  ("Reasonable" depends on what you're
trying do do - just keep in mind that the plugin properties end up as settings in proguard_processed.conf.)

* Look at the proguard_processed.conf file.  Does it make sense?  There should be -libraryjar lines for the standard Scala
libraries that your project uses, and -keep lines for all of your own code.

* Can you run proguard from a command line using the proguard_processed.conf file?  That file is a normal proguard configuration
and should work outside the plugin - there's nothing special or exotic about it.

# Release Notes

## v45

* Improved build process to avoid issues with categorization

## v44

* Fixed https://github.com/banshee/AndroidProguardScala/issues/22.  Defaults are now set correctly.  Before, defaults weren't set
until you visited the project settings page.
