name := "login"

version := "1.0.0"

scalaVersion := "2.13.16"
scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)


// We must include both AWS SDK V1 and V2 to enable the use of latest
// Scanamo whilst avoiding overhauling the whole app to V2.
val awsSdkVersion = "1.12.130"
val awsSdkVersionV2 = "2.34.5"
val jacksonVersion = "2.19.2"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  "com.gu" %% "pan-domain-auth-play_3-0" % "7.0.0",
  "com.gu.play-secret-rotation" %% "aws-parameterstore-sdk-v1" % "7.1.1",
  "com.gu.play-secret-rotation" %% "play-v30" % "7.1.1",
  "com.gu.etag-caching" %% "aws-s3-sdk-v2" % "8.2.0",
  "software.amazon.awssdk" % "ec2" % awsSdkVersionV2,
  "software.amazon.awssdk" % "cloudwatch" % awsSdkVersionV2,
  "software.amazon.awssdk" % "autoscaling" % awsSdkVersionV2,
  "software.amazon.awssdk" % "ses" % awsSdkVersionV2,
  "software.amazon.awssdk" % "ssm" % awsSdkVersionV2, //Needed for Parameter Store?
  "software.amazon.awssdk" % "kinesis" % awsSdkVersionV2,
  "software.amazon.awssdk" % "dynamodb" % awsSdkVersionV2,
  "net.logstash.logback" % "logstash-logback-encoder" % "7.3",
  "com.github.nscala-time" %% "nscala-time" % "2.32.0",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0",
  "org.scanamo" %% "scanamo" % "1.0.0-M17",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "com.gu" %% "anghammarad-client" % "1.8.1",
  // Jackson library version addressing vulnerable dependencies
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
)

def env(propName: String): Option[String] = sys.env.get(propName).filter(_.trim.nonEmpty)

lazy val mainProject = project.in(file("."))
  .enablePlugins(PlayScala, JDebPackaging, SystemdPlugin, BuildInfoPlugin)
  .settings(Defaults.coreDefaultSettings: _*)
  .settings(addCommandAlias("devrun", "run -Dconfig.resource=application.local.conf 9000"): _*)
  .settings(
    topLevelDirectory := Some(normalizedName.value),
    debianPackageDependencies := Seq("java11-runtime-headless"),
    maintainer := "Digital CMS <digitalcms.dev@guardian.co.uk>",
    packageSummary := "login.gutools",
    packageDescription := """Small application to login a user via pan-domain-auth and redirect them.""",

    Universal / javaOptions ++= Seq(
      "-Dpidfile.path=/dev/null"
    ),

    buildInfoPackage := "login",
    buildInfoKeys ++= Seq[BuildInfoKey](
      name,
      scalaVersion,
      sbtVersion,

      BuildInfoKey.sbtbuildinfoConstantEntry("buildNumber", env("BUILD_NUMBER")),
      // so this next one is constant to avoid it always recompiling on dev machines.
      // we only really care about build time on teamcity, when a constant based on when
      // it was loaded is just fine
      BuildInfoKey.sbtbuildinfoConstantEntry("buildTime", System.currentTimeMillis),
      BuildInfoKey.sbtbuildinfoConstantEntry("gitCommitId", env("GITHUB_SHA")),
    ),
    buildInfoOptions := Seq(
      BuildInfoOption.Traits("management.BuildInfo"),
      BuildInfoOption.ToJson
    )
  )
