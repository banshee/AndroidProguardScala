package com.restphone.androidproguardscala

//
//case class String(path: String) {
//  override def toString = path
//}
//
//case object String {
//  implicit def convertToString(p: String) = p.path
//}

case class ProguardCacheParameters(
  cacheDir: String,
  confDir: String,
  workspaceDir: String,
  projectDir: String,
  proguardAdditionsFile: String,
  proguardProcessedConfFile: String,
  cachedJar: String,
  outputJar: String,
  scalaLibraryJar: String,
  androidLibraryJar: String,
  classFiles: Array[String],
  extraLibs: Array[String],
  proguardDefaults: String,
  logger: ProvidesLogging)
