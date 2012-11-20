package com.restphone.androidproguardscala

import scala.util.control.Exception._
import java.io.File

object NotNull {
  val catchNull = catching(classOf[NullPointerException])

  def apply[T](x: => T, msg: String = "must not be null"): Option[T] = {
    catchNull.opt(x) match {
      case None | Some(null) => throw new RuntimeException(msg)
      case x => x
    }
  }
}

object RichFile {
  def slurp(f: File) = {
    val s = scala.io.Source.fromFile(f)
    val result = s.getLines.mkString("\n")
    s.close()
    result
  }
  def ensureDirExists(f: File) =
    if (!f.exists) f.mkdir
}
