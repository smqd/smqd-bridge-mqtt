
import sbt.Keys.{version, _}

val thisVersion = "0.4.0"
val smqdVersion = "0.4.0"
val akkaVersion = "2.5.13"
val alpakkaVersion = "0.19"

val `smqd-bridge-mqtt` = project.in(file(".")).settings(
  organization := "com.thing2x",
  name := "smqd-bridge-mqtt",
  version := thisVersion,
  scalaVersion := "2.12.6"
).settings(
  libraryDependencies ++= Seq(
    if (smqdVersion.endsWith("-SNAPSHOT"))
      "com.thing2x" %% "smqd-core" % smqdVersion changing() withSources()
    else
      "com.thing2x" %% "smqd-core" % smqdVersion
  ),
  resolvers += Resolver.sonatypeRepo("public")
).settings(
  // Publishing
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""), sys.env.getOrElse("SONATYPE_PASS", "")),
  homepage := Some(url("https://github.com/smqd/")),
  scmInfo := Some(ScmInfo(url("https://github.com/smqd/smqd-bridge-mqtt"), "scm:git@github.com:smqd/smqd-bridge-mqtt.git")),
  developers := List(
    Developer("OutOfBedlam", "Kwon, Yeong Eon", sys.env.getOrElse("SONATYPE_DEVELOPER_0", ""), url("http://www.uangel.com"))
  ),
  publishArtifact in Test := false, // Not publishing the test artifacts (default)
  publishMavenStyle := true
).settings(
  // PGP signing
  pgpPublicRing := file("./travis/local.pubring.asc"),
  pgpSecretRing := file("./travis/local.secring.asc"),
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  useGpg := false
).settings(
  //// Test
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
).settings(
  // License
  organizationName := "UANGEL",
  startYear := Some(2018),
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
  headerMappings := headerMappings.value + (HeaderFileType.conf -> HeaderCommentStyle.hashLineComment)
).enablePlugins(AutomateHeaderPlugin)
