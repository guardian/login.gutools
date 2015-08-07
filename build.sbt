name := """login"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,
  "com.gu" %% "pan-domain-auth-play_2-4-0" % "0.2.7",
  "com.amazonaws" % "aws-java-sdk" % "1.7.5"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
//routesGenerator := InjectedRoutesGenerator


sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false