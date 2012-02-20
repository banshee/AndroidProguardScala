package com.restphone.androidproguardscala

case class FullyQualifiedPath(path: String) {
  override def toString = path
}

case object FullyQualifiedPath {
  implicit def convertToString(p: FullyQualifiedPath) = p.path
}

case class ProguardCacheParameters(
  cacheDir: FullyQualifiedPath,
  confDir: FullyQualifiedPath,
  workspaceDir: FullyQualifiedPath,
  projectDir: FullyQualifiedPath,
  proguardAdditionsFile: FullyQualifiedPath,
  proguardProcessedConfFile: FullyQualifiedPath,
  cachedJar: FullyQualifiedPath,
  outputJar: FullyQualifiedPath,
  scalaLibraryJar: FullyQualifiedPath,
  androidLibraryJar: FullyQualifiedPath,
  classFiles: Array[FullyQualifiedPath],
  extraLibs: Array[FullyQualifiedPath],
  proguardDefaults: String,
  logger: ProvidesLogging)
