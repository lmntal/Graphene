package unyo.core

import scala.util.control.Exception.{allCatch}

object Env {

  import java.io.{File}
  import java.net.{URL}
  import java.awt.{Toolkit,Dimension}

  private val tk = Toolkit.getDefaultToolkit
  val frameWidth  = tk.getScreenSize.getWidth.toInt  * 2 / 3
  val frameHeight = tk.getScreenSize.getHeight.toInt * 2 / 3

  var isMultiCoreEnabled = false
  var isAntiAliasEnabled = true

  val jarRoot: String = {
    val cls = unyo.UNYO.getClass
    val classPath = cls.getResource(cls.getSimpleName + ".class").toExternalForm
    classPath.substring(0, classPath.lastIndexOf(cls.getPackage.getName.replace('.', '/')))
  }

  val property: Map[String,String] = {
    import scala.collection.JavaConversions._
    System.getProperties.toMap
  }

  val jarPath: String = new File(property("java.class.path")).getAbsolutePath

  val root: String = {
    val sep = property("file.separator")
    jarPath.split(sep).toList.init.mkString(sep) + sep
  }

  val manifest: Map[String,String] = try {
    import java.util.jar.Manifest
    import scala.collection.JavaConversions._

    val url = new java.net.URL(jarRoot + "META-INF/MANIFEST.MF")
    val is = url.openStream
    val manif = new Manifest(is)
    is.close

    val attrs = manif.getMainAttributes
    val map = collection.immutable.Map.newBuilder[String,String]
    for (key <- attrs.keySet) map += key.toString -> attrs.get(key).toString
    map.result
  } catch {
    case _: Exception => collection.immutable.Map.empty[String,String]
  }

  val version: Option[Version] = manifest.get("Implementation-Version").flatMap(Version.fromString)
}


object Meta {
  import scala.io.{Source}
  import scala.xml.{XML,Elem,NodeSeq}

  val infoUrl = "http://www.ueda.info.waseda.ac.jp/~yaguchi/unyo/info.xml"

  private def fetchInfoXML: Option[Elem] = allCatch.opt { XML.loadString(Source.fromURL(infoUrl).mkString) }

  private var cachedXML = Option.empty[Elem]
  private def infoXML: Option[Elem] = {
    if (cachedXML.isEmpty) cachedXML = fetchInfoXML
    cachedXML
  }

  private def searchLatestVersionText(elem: Elem): Option[String] =
    (elem \\ "app" \ "versions" \ "latest" \ "version").headOption.map { e => e.text }

  def latestVersion: Option[Version] = for {
    xml <- infoXML
    text <- searchLatestVersionText(xml)
    version <- Version.fromString(text)
  } yield version

  def needsUpdate = {
    val res = for {
      current <- Env.version
      latest <- latestVersion
    } yield current < latest
    res.getOrElse(false)
  }

}
