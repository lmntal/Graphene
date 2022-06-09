name := "Graphene"

version := "4.4.2"

scalaVersion := "2.11.12"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.12" % "test",
  "org.json4s" %% "json4s-native" % "4.0.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.specs2" %% "specs2-core" % "4.10.6" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
)

assembly / assemblyMergeStrategy ~= {
  old => {
    case "rootdoc.txt" => MergeStrategy.first
    case x             => old(x)
  }
}

