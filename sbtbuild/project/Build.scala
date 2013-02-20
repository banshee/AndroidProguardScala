import sbt._
import Keys._

object HelloBuild extends Build {
    lazy val root = Project(id = "aps",
                            base = file(".")) aggregate(scalaTestUtilities, javaSignatureParser, jartender, androidProguardScalaLib)

    lazy val javaSignatureParser = Project(id = "javaSignatureParser",
                           base = file("JavaSignatureParser"))

    lazy val jartender = Project(id = "jartender",
                           base = file("Jartender"))

    lazy val scalaTestUtilities = Project(id = "scalaTestUtilities",
                           base = file("ScalaTestUtilities"))

    lazy val androidProguardScalaLib = Project(id = "androidProguardScalaLib",
                           base = file("AndroidProguardScalaLib"))

}

