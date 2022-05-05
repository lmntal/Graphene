package graphene.core

import java.io.{File, FileReader, PrintWriter, FileWriter}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Properties {

  val logger = Logger(LoggerFactory.getLogger("Properties"))

  def load(filename: String): java.util.Properties = {
    val properties = new java.util.Properties
    try {
      //CHANGED ファイルを.jarのある位置の絶対パスで指定
      properties.load(new FileReader(graphene.core.Env.rootPath + filename))
    } catch {
      case e: java.io.FileNotFoundException => {
        try {
          properties.load(new FileReader(filename))
        } catch {
          case e2: java.io.FileNotFoundException => 
          logger.info("Cannot find {}. Use default properties", filename)
        }
      }
    }
    properties
  }

  def save(properties: java.util.Properties, filename: String) {
    properties.store(new FileWriter(filename), "Graphene properties")
  }
}
