import com.gu.riffraff.artifact.BuildInfo

name := "login"

version := "1.0.0"

scalaVersion := "2.13.7"
scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)


// We must include both AWS SDK V1 and V2 to enable the use of latest
// Scanamo whilst avoiding overhauling the whole app to V2.
val awsSdkVersion = "1.12.130"
val awsSdkVersionV2 = "2.17.101"

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  "com.gu" %% "pan-domain-auth-play_2-8" % "1.0.4",
  "com.gu.play-secret-rotation" %% "aws-parameterstore-sdk-v1" % "0.18",
  "com.gu.play-secret-rotation" %% "play-v28" % "0.31",
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-autoscaling" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ses" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-kinesis" % awsSdkVersion,
  "software.amazon.awssdk" % "dynamodb" % awsSdkVersionV2,
  "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
  "com.gu" % "kinesis-logback-appender" % "1.4.4",
  "com.github.nscala-time" %% "nscala-time" % "2.30.0",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0",
  "org.scanamo" %% "scanamo" % "1.0.0-M17",
  "org.scalatest" %% "scalatest" % "3.2.10" % Test,
  "com.gu" %% "anghammarad-client" % "1.2.0"
)

lazy val mainProject = project.in(file("."))
  .enablePlugins(PlayScala, RiffRaffArtifact, JDebPackaging, SystemdPlugin, BuildInfoPlugin)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(addCommandAlias("devrun", "run -Dconfig.resource=application.local.conf 9000"): _*)
  .settings(
    topLevelDirectory := Some(normalizedName.value),
    riffRaffPackageName := name.value,
    riffRaffManifestProjectName := s"editorial-tools:${name.value}",
    riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
    riffRaffUploadManifestBucket := Option("riffraff-builds"),
    riffRaffPackageType := (Debian / packageBin).value,

    riffRaffArtifactResources := Seq(
      (Debian / packageBin).value -> s"${name.value}/${name.value}.deb",
      file("riff-raff.yaml") -> "riff-raff.yaml"
    ),

    debianPackageDependencies := Seq("openjdk-8-jre-headless"),
    maintainer := "Digital CMS <digitalcms.dev@guardian.co.uk>",
    packageSummary := "login.gutools",
    packageDescription := """Small application to login a user via pan-domain-auth and redirect them.""",

    Universal / javaOptions ++= Seq(
      "-Dpidfile.path=/dev/null"
    ),

    buildInfoPackage := "login",
    buildInfoKeys := {
      lazy val buildInfo = BuildInfo(baseDirectory.value)
      Seq[BuildInfoKey](
        BuildInfoKey.constant("buildNumber", buildInfo.buildIdentifier),
        // so this next one is constant to avoid it always recompiling on dev machines.
        // we only really care about build time on teamcity, when a constant based on when
        // it was loaded is just fine
        BuildInfoKey.constant("buildTime", System.currentTimeMillis),
        BuildInfoKey.constant("gitCommitId", buildInfo.revision)
      )
    }
  )



