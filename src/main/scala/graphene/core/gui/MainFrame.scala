package graphene.core.gui

import java.awt.{Dimension, Toolkit}
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.event.{InputEvent, KeyEvent}

import javax.swing.{JFileChooser, JMenu, JMenuItem, KeyStroke, WindowConstants}
import javax.swing.filechooser.FileNameExtensionFilter
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import graphene.util._
import graphene.util.Geometry._
import graphene.core.{Env, Properties}
import graphene.model.Hot
import graphene.swing.scalalike._

object MainFrame {
  val instance = new MainFrame
}

//NOTE ウィンドウ本体
class MainFrame extends javax.swing.JFrame with JFrameExt {

  import javax.swing.{JMenuBar}

  closeOperation_ = javax.swing.WindowConstants.EXIT_ON_CLOSE

  this.setTitle("Graphene (Version: " + Env.version.map(_.toString).getOrElse("unknown") + ")")

  val mainPanel = new MainPanel

  //CHANGED ウィンドウサイズ設定を追加
  mainPanel.setPreferredSize(new Dimension(Env.frameWidth, Env.frameHeight))

  this << mainPanel

  menuBar_ = new JMenuBar with JMenuBarExt { //NOTE メニューバー

    this << new JMenu("File") with JMenuExt {
      mnemonic_ = KeyEvent.VK_F

      this << new JMenuItem("Open File") with JMenuItemExt {
        accelerator_ = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
        addActionListener(new ActionListener {
          override def actionPerformed(e: ActionEvent) = mainPanel.openFileChooser
        })
      }
    }
  }

  def runWithFile(file: String): Unit = {
    // FIXME
    mainPanel.graph = mainPanel.source.run(Seq(file))
  }
}

//NOTE アトム画面とメニュー画面両方
class MainPanel extends javax.swing.JPanel with JPanelExt {

  import graphene.plugin.lmntal.LMNtal

  val logger = Logger(LoggerFactory.getLogger("MainPanel"))

  val properties = Properties.load("graphene.properties")

  val plugin = LMNtal
  plugin.importProperties(Properties.load(plugin.name + ".properties"))
  val mover = plugin.mover
  val renderer = plugin.renderer
  val source = plugin.source
  val observer = plugin.observer
  val controlPanel = plugin.controlPanel

  var graph: plugin.GraphType = null
  val graphicsContext = new GraphicsContext

  layout_ = new java.awt.BorderLayout

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run {
      Properties.save(new java.util.Properties, "graphene.properties")
      //LMNtal.properties の読み込み
      Properties.save(plugin.exportProperties, plugin.name + ".properties")
    }
  })

  this << new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT) with JSplitPaneExt {
    leftComponent_ = new javax.swing.JPanel with JPanelExt { //アトム表示画面

      import java.awt.event.{KeyEvent}

      preferredSize_ = new Dimension(Env.frameWidth, Env.frameHeight)
      focusable_ = true

      var prevPoint: java.awt.Point = null
      listenToComponent
      listenToMouse
      listenToMouseMotion
      listenToMouseWheel
      listenToKey
      reactions += {
        case MousePressed(_, p, _, _, _) => requestFocusInWindow
        case KeyPressed(_, key, _, _) => if (key == KeyEvent.VK_SPACE && source.hasNext) graph = source.next
        case ComponentResized(_) => graphicsContext.resize(getSize)
      }
      reactions += observer.listener

      val t = new Thread { //NOTE 画面表示の更新プログラム
        override def run { 
          var prevMsec = System.currentTimeMillis
          while (true) {
            val msec = System.currentTimeMillis

            if (graph != null) mover.moveAll(graph, 1.0 * (msec - prevMsec) / 1000)
            if (!Hot.Always) Hot.Temperature -= (msec - prevMsec) * 0.1 //* 10
            if (Hot.Temperature < 0) Hot.Temperature = 0
            repaint()

            prevMsec = msec
            Thread.sleep(10) //TODO 良いSleepの値を見つける
          }
        }
      }
      t.start

      override def paintComponent(gg: java.awt.Graphics) {
        import java.awt.RenderingHints._

        val g = gg.asInstanceOf[java.awt.Graphics2D]

        super.paintComponent(g)

        g.clearRect(
          0,
          0,
          graphicsContext.sSize.width.toInt,
          graphicsContext.sSize.height.toInt
        )

        g.translate(graphicsContext.sSize.width / 2, graphicsContext.sSize.height / 2)
        g.scale(graphicsContext.magnificationRate, graphicsContext.magnificationRate)
        g.translate(-graphicsContext.wCenter.x, -graphicsContext.wCenter.y)

        if (Env.isAntiAliasEnabled) g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
        renderer.renderAll(g, graph)
      }
    }

    rightComponent_ = { //メニュー画面

      val tabbedPane = new javax.swing.JTabbedPane
      tabbedPane.addTab("General", new SettingPanel)
      tabbedPane.addTab(plugin.name, controlPanel)
      tabbedPane.addTab("Log", LogPanel)
      tabbedPane
    }

    //CHANGED 中央の分離バーの位置をピクセル単位で指定
    setDividerLocation(Env.frameWidth * 3 / 4)
  }

  def openFileChooser() = {
    val chooser = new JFileChooser(new java.io.File("~/")) with JFileChooserExt {
      fileFilter_ = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
    }
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        graph = source.run(Seq(chooser.selectedFile.getAbsolutePath))
      } catch {
        case e: java.io.IOException => logger.warn(e.getMessage)
      }
    }
  }
}

