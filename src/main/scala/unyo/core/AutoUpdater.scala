package unyo.core

object Checker {

  import scala.io.{Source}
  import scala.xml.{XML,Elem,NodeSeq}
  import scala.util.control.Exception.{allCatch}

  val infoUrl = "http://www.ueda.info.waseda.ac.jp/~yaguchi/unyo/info.xml"

  def fetchInfoXML: Option[Elem] = allCatch.opt { XML.loadString(Source.fromURL(infoUrl).mkString) }

  var cachedXML = Option.empty[Elem]
  def infoXML: Option[Elem] = {
    if (cachedXML.isEmpty) cachedXML = fetchInfoXML
    cachedXML
  }

  def searchLatestVersionText(elem: Elem): Option[String] =
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

object Updater {

  import javax.swing.{JPanel,JOptionPane}

  def attempt = if (Checker.needsUpdate) showDialog
  def showDialog = {
    val values: Array[Object] = Array("Cancel", "Update")
    JOptionPane.showOptionDialog(
      null,
      s"バージョン${Checker.latestVersion.get}が利用できます。更新しますか？",
      "Update Avaliable",
      JOptionPane.YES_NO_OPTION,
      JOptionPane. INFORMATION_MESSAGE,
      null,
      values,
      values(1)
    )
  }

}
