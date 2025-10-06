// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.9")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

libraryDependencies += "org.vafer" % "jdeb" % "1.10" artifacts (Artifact("jdeb", "jar", "jar"))
