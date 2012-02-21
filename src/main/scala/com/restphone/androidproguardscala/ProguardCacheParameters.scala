package com.restphone.androidproguardscala

case class ProguardCacheParameters(
  cacheDir: String,
  confDir: String,
  workspaceDir: String, // Provided for use in additional_libs - can be "" but not null 
  projectDir: String, // Provided for use in additional_libs - can be "" but not null 
  proguardAdditionsFile: String, // Path to file added to configuration - can be ""
  proguardProcessedConfFile: String, // The final proguard configuration file
  cachedJar: String, // Where to store cached minified jar file.  Needs to contain the string CKSUM.  (There will be many of these with different checksums)
  outputJar: String, // Final destination for the jar file (Only one of these, this is the jar included in the final android build)
  scalaLibraryJar: String, // path to scala-library.jar
  androidLibraryJar: String, // path to android.jar
  classFiles: Array[String], // directories and/or jar files; used in proguard -injar option
  extraLibs: Array[String], // passed to proguard -libraryjars
  proguardDefaults: String, // a string to add to the proguard conf file
  logger: ProvidesLogging)
