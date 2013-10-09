package unyo.gui

import scala.swing.{Frame,FlowPanel,FileChooser}
import scala.swing.{MenuBar,Menu,MenuItem,Action}
import scala.swing.{Panel,Graphics2D}

import java.awt.{Dimension}

import unyo.entity.Graph;
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
  import scala.swing.event.{MousePressed,MouseReleased,MouseDragged}
  import scala.swing.event.{KeyPressed,Key}
  import scala.actors.Actor._

  val visualGraph = new VisualGraph
  val graphicsContext = new GraphicsContext
  val mover = new DefaultMover

  preferredSize = new Dimension(Env.frameWidth, Env.frameHeight)
  focusable = true

  listenTo(this.keys, this.mouse.clicks, this.mouse.moves)
  var prevPoint: java.awt.Point = null
  reactions += {
    case KeyPressed(_, key, _, _) => if (key == Key.Space && runtime.hasNext) {
      val graph = runtime.next
      setGraph(graph)
    }
    case MousePressed(_, p, _, _, _) => prevPoint = p
    case MouseReleased(_, p, _, _, _) => prevPoint = null
    case MouseDragged(_, p, _) => if (prevPoint != null) {
      graphicsContext.moveBy(prevPoint - p)
      prevPoint = p
    }
  }

  actor {
    var prevMsec = System.currentTimeMillis
    loop {
      val msec = System.currentTimeMillis

      mover.move(visualGraph, 1.0 * (msec - prevMsec) / 100)
      repaint

      prevMsec = msec
      Thread.sleep(10)
    }
  }

  def setGraph(g: Graph) {
    visualGraph.rewrite(g)
    repaint
  }

  override def paint(g: Graphics2D) {
    import java.awt.RenderingHints._

    super.paint(g)

    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    val r = new DefaultRenderer(g, graphicsContext)
    r.render(visualGraph)
  }

  import unyo.runtime.LMNtalRuntime

  var runtime: LMNtalRuntime = null
  def openFileChooser {
    import javax.swing.filechooser.{FileFilter,FileNameExtensionFilter}

    val chooser = new FileChooser(new java.io.File("~/")) {
      fileFilter = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
    }
    val res = chooser.showOpenDialog(this)
    if (res == FileChooser.Result.Approve) {
      val file = chooser.selectedFile
      runtime = new LMNtalRuntime(file, java.util.Arrays.asList("-O", "--hide-rule", "--hide-ruleset"))
      val graph = runtime.next
      setGraph(graph)
    }
  }
}
