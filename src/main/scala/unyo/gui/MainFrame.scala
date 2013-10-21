package unyo.gui

import java.awt.{Dimension}

import unyo.util._
import unyo.util.Geometry._
import unyo.Env

import unyo.swing.scalalike._

object MainFrame {
  def instance = new MainFrame
}

class MainFrame extends javax.swing.JFrame with JFrameExt {
  import javax.swing.{JMenuBar}

  closeOperation_ = javax.swing.JFrame.EXIT_ON_CLOSE

  val graphPanel = new GraphPanel
  this << graphPanel

  menuBar_ = new JMenuBar with JMenuBarExt {
    import java.awt.event.{ActionListener,ActionEvent}
    import java.awt.event.{KeyEvent,InputEvent}
    import javax.swing.{JMenu,JMenuItem,KeyStroke}

    this << new JMenu("File") with JMenuExt {
      mnemonic_ = KeyEvent.VK_F

      this << new JMenuItem("Open File") with JMenuItemExt {
        accelerator_ = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK)
        addActionListener(new ActionListener {
          override def actionPerformed(e: ActionEvent) = graphPanel.openFileChooser
        })
      }
    }
  }
}

class GraphPanel extends javax.swing.JPanel with JPanelExt {
  import scala.actors.Actor._
  import unyo.plugin.lmntal.LMNtalPlugin

  val plugin = LMNtalPlugin
  var visualGraph: plugin.GraphType = null
  val graphicsContext = new GraphicsContext
  val mover = plugin.movers(0)
  val renderer = plugin.renderers(0)
  val runtime = plugin.runtimes(0)
  val observer = plugin.observers(0)

  preferredSize_ = new Dimension(Env.frameWidth, Env.frameHeight)
  focusable_ = true

  import java.awt.event.{MouseListener,MouseMotionListener,MouseEvent}
  import java.awt.event.{MouseWheelListener,MouseWheelEvent}
  import java.awt.event.{KeyListener,KeyEvent}
  import java.awt.event.{ComponentListener,ComponentEvent}

  var prevPoint: java.awt.Point = null
  listenToComponent
  listenToMouse
  listenToMouseMotion
  listenToMouseWheel
  listenToKey
  reactions += {
    case MousePressed(_, p, _, _, _) => if (observer.canMoveScreen) prevPoint = p
    case MouseReleased(_, p, _, _, _) => if (observer.canMoveScreen) prevPoint = null
    case MouseDragged(_, p, _) => if (observer.canMoveScreen && prevPoint != null) {
      graphicsContext.moveBy(prevPoint - p)
      prevPoint = p
    }
    case MouseWheelMoved(_, _, _, rot) => graphicsContext.zoom(math.pow(1.01, rot))
    case KeyPressed(_, key, _, _) => if (key == KeyEvent.VK_SPACE && runtime.hasNext) visualGraph = runtime.next
    case ComponentResized(_) => graphicsContext.resize(getSize)
  }

  actor {
    var prevMsec = System.currentTimeMillis
    loop {
      val msec = System.currentTimeMillis

      if (visualGraph != null) mover.moveAll(visualGraph, 1.0 * (msec - prevMsec) / 100)
      repaint()

      prevMsec = msec
      Thread.sleep(10)
    }
  }

  override def paintComponent(gg: java.awt.Graphics) {
    import java.awt.RenderingHints._

    val g = gg.asInstanceOf[java.awt.Graphics2D]

    super.paintComponent(g)

    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    renderer.renderAll(g, graphicsContext, visualGraph)
  }

  import unyo.plugin.lmntal.LMNtalRuntime

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
