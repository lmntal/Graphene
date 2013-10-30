package unyo

import org.json4s._
import org.json4s.native.Serialization.{read,writePretty}
import java.io.{File,PrintWriter,FileWriter}

object Settings {

  val settingsPath = "settings.json"

  private def readFile(file: File) = io.Source.fromFile(file).mkString
  private def parseSettings(text: String): Map[String,Map[String,String]] = {
    implicit val formats = org.json4s.native.Serialization.formats(NoTypeHints)
    read[Map[String,Map[String,String]]](text)
  }
  
  def load: Map[String,Map[String,String]] = {
    val file = new File(settingsPath)
    val settings = {
      try {
        if (file.canRead) parseSettings(readFile(file)) else Map.empty[String,Map[String,String]]
      } catch {
        case _: Throwable => {
          println("Failed to load " + settingsPath + ".")
          println("Use default settings.")
          Map.empty[String,Map[String,String]]
        }
      }
    }
    settings.withDefault(_ => Map.empty[String,String])
  }

  def save(settings: Map[String,Map[String,String]]) {
    implicit val formats = org.json4s.native.Serialization.formats(NoTypeHints)
    val text = writePretty(settings)
    val writer = new PrintWriter(new FileWriter(settingsPath))
    writer.println(text)
    writer.close
  }
}
