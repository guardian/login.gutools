//import com.typesafe.sbt.packager.Keys._

name := "login"

version := "1.0.0"

val awsSdkVersion = "1.10.72"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.gu" %% "pan-domain-auth-play_2-4-0" % "0.2.13",
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk" % awsSdkVersion,
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "io.megl" %% "play-json-extra" % "2.4.3",
  "com.github.t3hnar" % "scala-bcrypt_2.11" % "2.6",
  "com.gu" %% "scanamo" % "0.4.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scalaVersion := "2.11.8"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
//routesGenerator := InjectedRoutesGenerator


sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

lazy val mainProject = project.in(file("."))
  .enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(addCommandAlias("devrun", "run -Dconfig.resource=application.local.conf 9000"): _*)
  .settings(
    topLevelDirectory := Some(normalizedName.value),
    riffRaffPackageName := name.value,
    riffRaffManifestProjectName := s"editorial-tools:${name.value}",
    riffRaffBuildIdentifier :=  Option(System.getenv("CIRCLE_BUILD_NUM")).getOrElse("dev"),
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffManifestBranch := Option(System.getenv("CIRCLE_BRANCH")).getOrElse("dev"),
    riffRaffPackageType := (packageBin in Debian).value)
  .settings(
    javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null"
    )
  )

import com.typesafe.sbt.packager.archetypes.ServerLoader.Systemd
serverLoading in Debian := Systemd
debianPackageDependencies := Seq("openjdk-8-jre-headless")
maintainer := "Digital CMS <digitalcms.dev@guardian.co.uk>"
packageSummary := "login.gutools"
packageDescription := """Small application to login a user via pan-domain-auth and redirect them."""



