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

# Managed dependencies

As long as your dependency resolution tool puts libraries on the classpath, this plugin can use them.

## Ivy

I use [IvyDE](http://ant.apache.org/ivy/ivyde/) to manage dependencies in Eclipse.
IvyDE puts dependencies in its own classpath container, so you can just mark them as input jars
and they'll end up in scala-library.min.jar.  Nothing to configure other than marking the jars correctly in the plugin preferences.
See the sbt section for how to turn sbt dependencies into something that IvyDE can use.

## sbt

sbt will generate ivy dependency files that can be read by IvyDE.  Just run deliver-local in sbt and it'll build an ivy xml file in
something like target/scala-2.10/ivy-0.4-SNAPSHOT.xml.  Point IvyDE to that file and you'll end up with dependencies on your
classpath that can be used by the plugin.

I don't actually use that ivy file though, since sbt can delete it.  I copy target/scala-2.10/ivy-0.4-SNAPSHOT.xml into the
project root and name it ivy.xml.  It's an extra step (that can probably be automated inside sbt, but I've never bothered to learn 
how) but it avoids problems with the sbt build removing target/scala-2.10/ivy-0.4-SNAPSHOT.xml.

## Maven

TBD.  The problem with m2eclipse is that it takes over lots of the build process, so I never got it working nicely with
AndroidProguardScala.

# Troubleshooting

* If you get a scala-library.min.jar, but it's empty, make sure you have a resonable set of libraries configured in the 
plugin properties.  Right click on your project and select AndroidProguardScala properties.  ("Reasonable" depends on what you're
trying do do - just keep in mind that the plugin properties end up as settings in proguard_processed.conf.)

* Look at the proguard_processed.conf file.  Does it make sense?  There should be -libraryjar lines for the standard Scala
libraries that your project uses, and -keep lines for all of your own code.

* Can you run proguard from a command line using the proguard_processed.conf file?  That file is a normal proguard configuration
and should work outside the plugin - there's nothing special or exotic about it.

# Reporting bugs

If you've got a failing project, the very best thing for me is a tarball of the entire project,
including all the built binaries.  Yes, this is big, but most android projects will be less than a gigabyte.

The second-best thing is all of the source files minus the binaries.  Definitely include all the files I need to 
bring this up as an Eclipse project.

The third-best is something like an sbt project where I can run 'sbt eclipse'.  The problem with this is that 
sbt-eclipse doesn't always produce a working project, so I'm less likely to be able to take the time to get it working.

If you can't do that, a copy of the generated proguard config file is helpful, along with any errors you see in the 
Eclipse error log.

# Release Notes

## v48 (in development - you'll need to build it yourself to see these changes)

* Fixed license files and xml

## v47 (current release)

* An Eclipse clean build now cleans the AndroidProguardScala cache
* Added the plugin version number to the generated Proguard config file (should make tracking down bugs easier)
* Write the name of the cached library to the Eclipse log on a build, don't just report a cache hit or miss

## v46

* Changed Proguard defaults to include fix for https://issues.scala-lang.org/browse/SI-5397 
* Updated to Scala release 2.10
* Default to ignore scala-swing.jar
* Internal build improvements

## v45

* Improved build process to avoid issues with categorization

## v44

* Fixed https://github.com/banshee/AndroidProguardScala/issues/22.  Defaults are now set correctly.  Before, defaults weren't set
until you visited the project settings page.
