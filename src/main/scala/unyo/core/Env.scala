package unyo.core

import java.awt.{Toolkit,Dimension}

object Version {
  def fromString(s: String): Option[Version] = try {
    Some(Version(s.split('.').map(_.toInt)))
  } catch {
    case _: Exception => Option.empty[Version]
  }

  def apply(vs: Seq[Int]) = new Version(vs(0), vs(1), vs(2))
}

case class Version(major: Int, minor: Int, patch: Int) {
  override def toString = s"$major.$minor.$patch"
}

object Env {
  private val tk = Toolkit.getDefaultToolkit
  val frameWidth  = tk.getScreenSize.getWidth.toInt  * 2 / 3
  val frameHeight = tk.getScreenSize.getHeight.toInt * 2 / 3

  var isMultiCoreEnabled = false
  var isAntiAliasEnabled = true

  val jarPath = {
    val cls = unyo.UNYO.getClass
    val classPath = cls.getResource(cls.getSimpleName + ".class").toExternalForm
    classPath.substring(0, classPath.lastIndexOf(cls.getPackage.getName.replace('.', '/')))
  }

  val manifest: Map[String,String] = try {
    import java.util.jar.Manifest
    import scala.collection.JavaConversions._

    val url = new java.net.URL(jarPath + "META-INF/MANIFEST.MF")
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
