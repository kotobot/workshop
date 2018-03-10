name := "workshop"

organization := "com.grammarly"

version := "0.1"

scalaVersion := "2.11.10"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

fork in Test := true

resolvers += Resolver.mavenLocal

libraryDependencies ++=
  Seq(
    "org.apache.spark" %% "spark-core" % "2.2.1" % "provided",
    "org.apache.spark" %% "spark-sql" % "2.2.1" % "provided",
    "org.apache.spark" %% "spark-mllib" % "2.2.1" % "provided",
    "com.martinkl.warc" % "warc-hadoop" % "0.1.0-silent",
    "com.optimaize.languagedetector" % "language-detector" % "0.7-no-guava",
    "com.johnsnowlabs.nlp" %% "spark-nlp" % "1.4.1",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )

/*assemblyMergeStrategy in assembly := {
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("com.google.**" -> "shaded.google.@1").inAll
)*/
