package unyo.gui

import scala.swing.{Frame,FlowPanel,FileChooser}
import scala.swing.{MenuBar,Menu,MenuItem,Action}
import scala.swing.{Panel,Graphics2D}

import java.awt.{Dimension}

import unyo.util._
import unyo.util.Geometry._
import unyo.Env

object MainFrame {
  def instance = new MainFrame
}

class MainFrame extends Frame {
  import scala.swing.event.Key
  import java.awt.event.{KeyEvent,InputEvent}
  import javax.swing.KeyStroke

  override def closeOperation = dispose

  val graphPanel = new GraphPanel
  contents = graphPanel

  menuBar = new MenuBar {
    contents += new Menu("File") {
      mnemonic = Key.F

      val fileAction = Action("Open File") { graphPanel.openFileChooser }
      fileAction.accelerator = Option(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
      contents += new MenuItem(fileAction) { mnemonic = Key.O }
    }
  }
}

class GraphPanel extends Panel {
  import scala.swing.event.{MousePressed,MouseReleased,MouseDragged,MouseWheelMoved}
  import scala.swing.event.{KeyPressed,Key}
  import scala.swing.event.{UIElementResized}
  import scala.actors.Actor._
  import unyo.plugin.lmntal.LMNtalPlugin

  val plugin = LMNtalPlugin
  var visualGraph: plugin.GraphType = null
  val graphicsContext = new GraphicsContext
  val mover = plugin.movers(0)
  val renderer = plugin.renderers(0)
  val runtime = plugin.runtimes(0)
  val observer = plugin.observers(0)

  preferredSize = new Dimension(Env.frameWidth, Env.frameHeight)
  focusable = true

  listenTo(this.keys, this.mouse.clicks, this.mouse.moves, this.mouse.wheel, this)
  var prevPoint: java.awt.Point = null
  reactions += {
    case UIElementResized(_) => graphicsContext.resize(this.size)
    case KeyPressed(_, key, _, _) => if (key == Key.Space && runtime.hasNext) {
      visualGraph = runtime.next
      repaint
    }
    case MousePressed(_, p, _, _, _) => if (observer.canMoveScreen) prevPoint = p
    case MouseReleased(_, p, _, _, _) => if (observer.canMoveScreen) prevPoint = null
    case MouseDragged(_, p, _) => if (observer.canMoveScreen && prevPoint != null) {
      graphicsContext.moveBy(prevPoint - p)
      prevPoint = p
    }
    case MouseWheelMoved(_, _, _, rotation) => {
      graphicsContext.magnificationRate *= math.pow(1.01, rotation)
    }
  }
  reactions += observer.listenOn(graphicsContext)

  actor {
    var prevMsec = System.currentTimeMillis
    loop {
      val msec = System.currentTimeMillis

      if (visualGraph != null) mover.moveAll(visualGraph, 1.0 * (msec - prevMsec) / 100)
      repaint

      prevMsec = msec
      Thread.sleep(10)
    }
  }

  override def paint(g: Graphics2D) {
    import java.awt.RenderingHints._

    super.paint(g)

    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    renderer.renderAll(g, graphicsContext, visualGraph)
  }

  import unyo.plugin.lmntal.LMNtalRuntime

  def openFileChooser {
    import javax.swing.filechooser.{FileFilter,FileNameExtensionFilter}

    val chooser = new FileChooser(new java.io.File("~/")) {
      fileFilter = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
    }
    val res = chooser.showOpenDialog(this)
    if (res == FileChooser.Result.Approve) {
      val file = chooser.selectedFile
      visualGraph = runtime.exec(Seq(file.getAbsolutePath))
      repaint
    }
  }
}
