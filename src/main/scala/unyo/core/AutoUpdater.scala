package unyo.core

import scala.util.control.Exception.{allCatch}

import com.typesafe.scalalogging.slf4j._

import unyo.util._

object Downloader {
  def apply(url: java.net.URL) = new Downloader(url)
}

class Downloader(url: java.net.URL) {

  import java.io.{ByteArrayOutputStream}
  import java.net.{URL,URLConnection}

  private val blockSize = 2048
  private var progress: Double => Unit = _
  private var complete: Array[Byte] => Unit = _
  private var failure: Exception => Unit = _

  def onProgress(f: Double => Unit): Downloader = { progress = f; this }
  def onComplete(f: Array[Byte] => Unit): Downloader = { complete = f; this }
  def onFailure(f: Exception => Unit): Downloader = { failure = f; this }

  def start: Unit = {
    val conn = url.openConnection
    val length = conn.getContentLength
    val in = conn.getInputStream
    val out = new ByteArrayOutputStream(length)

    var done = false
    var off = 0
    while (!done) {
      progress(1.0 * off / length)

      val buf = new Array[Byte](blockSize)
      val len = in.read(buf)

      if (len == -1) {
        done = true
      } else {
        out.write(buf, 0, len)
        off += len
      }
    }
    progress(1.0 * off / length)
    complete(out.toByteArray)
  }

}

private class UpdaterFrame extends javax.swing.JFrame {

  import java.awt.{Dimension,FlowLayout}
  import javax.swing.{JLabel,JProgressBar,JButton}

  private val label = new JLabel("Downloading...") {
    setPreferredSize(new Dimension(360, 40))
  }
  private val progressBar = new JProgressBar {
    setPreferredSize(new Dimension(360, 80))
    setMinimum(0)
    setMaximum(100)
  }
  private val button = new JButton("Restart") {
    setEnabled(false)
  }

  def progress = progressBar.getValue.toDouble / 100
  def progress_=(per: Double) = progressBar.setValue((per * 100).toInt)

  def complete: Unit = {
    progressBar.setValue(100)
    button.setEnabled(true)
    button.requestFocusInWindow
  }

  def onButtonPressed(f: => Unit): Unit = {
    import java.awt.event.{ActionListener,ActionEvent}
    button.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = f
    })
  }

  setLayout(new FlowLayout)
  setPreferredSize(new Dimension(400, 200))

  add(label)
  add(progressBar)
  add(button)

}

object Updater extends Logging {

  import java.io.{File,FileOutputStream}
  import java.nio.file.{Paths,Files,StandardCopyOption}
  import java.net.{URL}
  import javax.swing.{JPanel,JOptionPane}

  import scala.actors.Actor.actor
  import unyo.util.Tapper._

  val jarName = "unyo.jar"
  val newJarName = "unyo-latest.jar"

  val defaultJar = new File(Env.rootPath + jarName)
  def noDefaultJarLog = logger.info("{} does not exist", defaultJar.getAbsolutePath)

  def runAsync = actor { run }

  def run: Unit = for (latest <- Meta.latestRelease) if (
    Meta.needsUpdate &&
    defaultJar.exists.tap { b => if (!b) noDefaultJarLog } &&
    confirmDialog(latest)
  ) update(latest)

  def confirmDialog(release: Release): Boolean = {
    val values: Array[Object] = Array("Cancel", "Update")
    val res = JOptionPane.showConfirmDialog(
      null,
      s"バージョン${release.version}が利用できます。更新しますか？",
      "Update Avaliable",
      JOptionPane. OK_CANCEL_OPTION
    )
    res == JOptionPane.OK_OPTION
  }

  def download(url: URL, file: File, frame: UpdaterFrame) = {
    Downloader(url).onProgress { per =>
      frame.progress = per
    }.onComplete { res =>
      using(new FileOutputStream(file)) { _.write(res) }
      frame.complete
    }.start
  }

  def update(release: Release): Unit = {
    val frame = new UpdaterFrame
    frame.onButtonPressed {
      replaceJar
      restart
    }
    frame.setVisible(true)
    frame.pack
    frame.setLocationRelativeTo(null)

    val url = release.url
    val file = new File(Env.rootPath + newJarName)
    download(url, file, frame)
  }

  def replaceJar = {
    val current = Paths.get(Env.rootPath + jarName)
    val latest = Paths.get(Env.rootPath + newJarName)

    Files.move(latest, current, StandardCopyOption.REPLACE_EXISTING)
  }

  def restart = {
    val command = s"java -jar ${Env.rootPath}/${jarName}"
    logger.info("run process: " + command)
    sys.process.Process(command).run
    sys.exit(0)
  }

}
