import com.gu.riffraff.artifact.BuildInfo

name := "login"

version := "1.0.0"

scalaVersion := "2.12.5"
scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Ypartial-unification"
)

val awsSdkVersion = "1.11.678"

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  "com.gu" %% "pan-domain-auth-play_2-6" % "0.7.0",
  "com.gu.play-secret-rotation" %% "aws-parameterstore-sdk-v1" % "0.14",
  "com.amazonaws" % "aws-java-sdk-ec2" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-autoscaling" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-ses" % awsSdkVersion,
  "com.amazonaws" % "aws-java-sdk-kinesis" % awsSdkVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % "6.0",
  "com.gu" % "kinesis-logback-appender" % "1.4.4",
  "com.github.nscala-time" %% "nscala-time" % "2.18.0",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
  "com.gu" %% "scanamo" % "1.0.0-M6",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.gu" %% "anghammarad-client" % "1.1.3"
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
    riffRaffPackageType := (packageBin in Debian).value,

    riffRaffArtifactResources := Seq(
      (packageBin in Debian).value -> s"${name.value}/${name.value}.deb",
      file("riff-raff.yaml") -> "riff-raff.yaml"
    ),

    debianPackageDependencies := Seq("openjdk-8-jre-headless"),
    maintainer := "Digital CMS <digitalcms.dev@guardian.co.uk>",
    packageSummary := "login.gutools",
    packageDescription := """Small application to login a user via pan-domain-auth and redirect them.""",

    javaOptions in Universal ++= Seq(
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



