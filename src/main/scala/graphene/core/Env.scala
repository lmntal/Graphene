package graphene.core

import java.net.{URL, URI}
import scala.util.control.Exception.{allCatch}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Env {

  import java.io.{File}
  import java.awt.{Toolkit}

  import scala.jdk.CollectionConverters._

  val logger = Logger(LoggerFactory.getLogger("Env"))

  private val tk = Toolkit.getDefaultToolkit
  val frameWidth  = tk.getScreenSize.getWidth.toInt  * 2 / 3
  val frameHeight = tk.getScreenSize.getHeight.toInt * 2 / 3

  var isMultiCoreEnabled = false
  var isAntiAliasEnabled = true

  val property: Map[String,String] = System.getProperties.asScala.toMap

  val jarRootPath: String = {
    val cls = graphene.Graphene.getClass
    val classPath = cls.getResource(cls.getSimpleName + ".class").toExternalForm
    classPath.substring(0, classPath.lastIndexOf(cls.getPackage.getName.replace('.', '/')))
  }

  val jarFilePath: String = new File(property("java.class.path")).getAbsolutePath

  val rootPath: String = {
    val sep = property("file.separator")
    jarFilePath.split(sep).toList.init.mkString(sep) + sep
  }

  val manifest: Map[String,String] = {

    try {
      // Try to get the manifest from the jar file directly
      val jarFile = new java.io.File(jarFilePath)
      if (jarFile.exists()) {
        val jar = new java.util.jar.JarFile(jarFile)
        val manifest = jar.getManifest
        if (manifest != null) {
          val attrs = manifest.getMainAttributes.asScala.toSeq.map { e => (e._1.toString, e._2.toString) }.toMap
          logger.debug("Jar manifest attributes: {}", attrs)
          attrs
        } else {
          Map.empty[String,String]
        }
      } else {
        Map.empty[String,String]
      }
    } catch {
      case e: Exception => 
        logger.error("Failed to load manifest: {}", e.getMessage)
        Map.empty[String,String]
    }
  }

  val version: Option[Version] = manifest.get("Implementation-Version").flatMap { Version.fromString }

  logger.debug("jar root path: {}", jarRootPath)
  logger.debug("jar file path: {}", jarFilePath)
  logger.debug("root path: {}", rootPath)
}


object Release {

  import scala.xml.{NodeSeq}
  def fromXML(node: NodeSeq): Option[Release] = for {
    version <- (node \ "version").headOption.flatMap { n => Version.fromString(n.text) }
    updated <- (node \ "updated").headOption.map { _.text }
    url     <- (node \ "url").headOption.map { n => URI.create(n.text).toURL }
    description <- (node \ "description").headOption.map { _.text.trim }
  } yield Release(version, updated, url, description)

}

case class Release(
  version: Version,
  updated: String,
  url: URL,
  description: String
)

object Meta {

  import scala.io.{Source}
  import scala.xml.{XML,Elem,NodeSeq}

  val infoUrl = "http://www.ueda.info.waseda.ac.jp/~yaguchi/unyo/info.xml"

  val logger = Logger(LoggerFactory.getLogger("Meta"))

  private def infoXML: Option[Elem] = allCatch.opt { XML.loadString(Source.fromURL(infoUrl).mkString) }
  private def latestReleaseNode: Option[NodeSeq] = infoXML.flatMap { xml => (xml \\ "app" \ "releases" \ "latest").headOption }
  val latestRelease: Option[Release] = latestReleaseNode.flatMap { node => Release.fromXML(node) }

  logger.debug("latest release: {}", latestRelease)

  def needsUpdate = {
    val res = for {
      current <- Env.version
      latest <- latestRelease.map { _.version }
    } yield current < latest
    res.getOrElse(false)
  }

}
