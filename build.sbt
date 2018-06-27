
import sbt.Keys.{version, _}
import scala.sys.process._

val smqdVersion = "0.1.0"
val akkaVersion = "2.5.13"
val alpakkaVersion = "0.19"

lazy val gitBranch = "git rev-parse --abbrev-ref HEAD".!!.trim
lazy val gitCommitShort = "git rev-parse HEAD | cut -c 1-7".!!.trim
lazy val gitCommitFull = "git rev-parse HEAD".!!.trim

val versionFile       = s"echo version = $smqdVersion" #> file("src/main/resources/smqd-bridge-mqtt-version.conf") !
val commitVersionFile = s"echo commit-version = $gitCommitFull" #>> file("src/main/resources/smqd-bridge-mqtt-version.conf") !

val `smqd-bridge-mqtt` = project.in(file(".")).settings(
  organization := "t2x.smqd",
  name := "smqd-bridge-mqtt",
  version := smqdVersion,
  scalaVersion := "2.12.6"
).settings(
  libraryDependencies ++= Seq(
    "t2x.smqd" %% "smqd-core" % smqdVersion
  )
).settings(
  // Publishing
  publishTo := Some(
    "bintray" at "https://api.bintray.com/maven/smqd/"+"smqd/smqd-bridge-mqtt_2.12/;publish=1"),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  publishMavenStyle := true,
  resolvers += Resolver.bintrayRepo("smqd", "smqd")
).settings(
  //// Test
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
)
