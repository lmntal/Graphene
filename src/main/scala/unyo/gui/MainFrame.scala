package unyo.gui

import java.awt.{Dimension}

import unyo.utility._
import unyo.utility.Geometry._
import unyo.Env
import unyo.Properties

import unyo.swing.scalalike._

object MainFrame {
  def instance = new MainFrame
}

class MainFrame extends javax.swing.JFrame with JFrameExt {
  import javax.swing.{JMenuBar}

  closeOperation_ = javax.swing.JFrame.EXIT_ON_CLOSE

  val mainPanel = new MainPanel
  this << mainPanel

  menuBar_ = new JMenuBar with JMenuBarExt {
    import java.awt.event.{ActionListener,ActionEvent}
    import java.awt.event.{KeyEvent,InputEvent}
    import javax.swing.{JMenu,JMenuItem,KeyStroke}

    this << new JMenu("File") with JMenuExt {
      mnemonic_ = KeyEvent.VK_F

      this << new JMenuItem("Open File") with JMenuItemExt {
        accelerator_ = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK)
        addActionListener(new ActionListener {
          override def actionPerformed(e: ActionEvent) = mainPanel.openFileChooser
        })
      }
    }
  }
}

class MainPanel extends javax.swing.JPanel with JPanelExt {

  import unyo.plugin.lmntal.LMNtalPlugin

  val properties = Properties.load("unyo.properties")

  val plugin = LMNtalPlugin
  plugin.importProperties(Properties.load(plugin.name + ".properties"))
  val mover = plugin.mover
  val renderer = plugin.renderer
  val runtime = plugin.runtime
  val observer = plugin.observer
  val controlPanel = plugin.controlPanel

  var visualGraph: plugin.GraphType = null
  val graphicsContext = new GraphicsContext

  layout_ = new java.awt.BorderLayout


  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run {
      Properties.save(new java.util.Properties, "unyo.properties")
      Properties.save(plugin.exportProperties, plugin.name + ".properties")
    }
  })

  this << new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT) with JSplitPaneExt {
    leftComponent_ = new javax.swing.JPanel with JPanelExt {
      import scala.actors.Actor._
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
        case MousePressed(_, p, _, _, _) => {
          requestFocusInWindow
          if (observer.canMoveScreen) prevPoint = p
        }
        case MouseReleased(_, p, _, _, _) => if (observer.canMoveScreen) prevPoint = null
        case MouseDragged(_, p, _) => if (observer.canMoveScreen && prevPoint != null) {
          graphicsContext.moveBy(prevPoint - p)
          prevPoint = p
        }
        case MouseWheelMoved(_, p, _, rot) => graphicsContext.zoom(math.pow(1.01, rot), p)
        case KeyPressed(_, key, _, _) => if (key == KeyEvent.VK_SPACE && runtime.hasNext) visualGraph = runtime.next
        case ComponentResized(_) => graphicsContext.resize(getSize)
      }
      reactions += observer.listenOn(graphicsContext)

      actor {
        var prevMsec = System.currentTimeMillis
        loop {
          val msec = System.currentTimeMillis

          if (visualGraph != null) mover.moveAll(visualGraph, 1.0 * (msec - prevMsec) / 1000)
          repaint()

          prevMsec = msec
          Thread.sleep(10)
        }
      }

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

        g.translate(graphicsContext.sSize.width/2, graphicsContext.sSize.height/2)
        g.scale(graphicsContext.magnificationRate, graphicsContext.magnificationRate)
        g.translate(-graphicsContext.wCenter.x, -graphicsContext.wCenter.y)

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
        renderer.renderAll(g, graphicsContext, visualGraph)
      }

    }
    rightComponent_ = controlPanel
  }

  def openFileChooser {
    import javax.swing.{JFileChooser}
    import javax.swing.filechooser.{FileNameExtensionFilter}

    val chooser = new JFileChooser(new java.io.File("~/")) with JFileChooserExt {
      fileFilter_ = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
    }
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      visualGraph = runtime.exec(Seq(chooser.selectedFile.getAbsolutePath))
    }
  }
}

