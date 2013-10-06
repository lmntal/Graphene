import AssemblyKeys._

name := "UNYO UNYO"

version := "4.0.0"

scalaVersion := "2.10.2"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "org.json4s" %% "json4s-native" % "3.2.2"
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
