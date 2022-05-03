name := "Graphene"

version := "4.4.0"

scalaVersion := "2.10.7"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.12" % "test",
  "org.json4s" %% "json4s-native" % "3.6.12",
  "com.typesafe" %% "scalalogging-slf4j" % "1.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.specs2" %% "specs2-core" % "3.10.0" % "test"
)

libraryDependencies += (scalaVersion (
   "org.scala-lang" % "scala-actors" % _
)).value

mergeStrategy in assembly ~= {
  old => {
    case "rootdoc.txt" => MergeStrategy.first
    case x             => old(x)
  }
}
