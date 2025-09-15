name := "Graphene"

version := "4.4.3"

scalaVersion := "2.13.13"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % "test",
  "org.json4s" %% "json4s-native" % "4.0.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.slf4j" % "slf4j-api" % "2.0.16",
  "ch.qos.logback" % "logback-classic" % "1.5.12",
  "org.specs2" %% "specs2-core" % "4.20.8" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "2.3.0"
)

assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case "rootdoc.txt" => MergeStrategy.first
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

packageOptions := Seq(
  Package.ManifestAttributes(
    "Implementation-Version" -> version.value,
    "Main-Class" -> "graphene.Graphene"
  )
)

