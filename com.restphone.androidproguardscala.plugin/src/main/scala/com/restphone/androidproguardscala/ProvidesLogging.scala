package com.restphone.androidproguardscala

trait ProvidesLogging {
  def logMsg(msg: String)
  def logError(msg: String)
}

object ProvidesLogging {
  val NullLogger = new ProvidesLogging {
    def logMsg(msg: String) {}
    def logError(msg: String) {}
  }
}