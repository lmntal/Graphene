import AssemblyKeys._

name := "Graphene"

version := "4.3.1"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "org.json4s" %% "json4s-native" % "3.2.6",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.specs2" %% "specs2" % "2.3.7" % "test"
)

libraryDependencies <+= scalaVersion {
   "org.scala-lang" % "scala-actors" % _
}

assemblySettings

mergeStrategy in assembly ~= {
  old => {
    case "rootdoc.txt" => MergeStrategy.first
    case x             => old(x)
  }
}
