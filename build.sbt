name := "workshop"

organization := "com.grammarly"

version := "0.1"

scalaVersion := "2.11.10"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

fork in Test := true

resolvers += Resolver.mavenLocal

val sparkVersion = "2.3.0"

libraryDependencies ++=
  Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
    "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
    "com.martinkl.warc" % "warc-hadoop" % "0.1.0-silent",
    "com.optimaize.languagedetector" % "language-detector" % "0.7-no-guava",
    "com.johnsnowlabs.nlp" %% "spark-nlp" % "1.5.3",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  )

assemblyMergeStrategy in assembly := {
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}

assemblyShadeRules in assembly := Seq(
  ShadeRule.rename("com.google.**" -> "shaded.google.@1").inAll
)
