package unyo.gui

import scala.swing.{Panel,Graphics2D}
import scala.swing.event.{MousePressed,MouseReleased,MouseDragged}
import scala.actors.Actor._

import unyo.entity.Graph;
import unyo.util._
import unyo.util.Geometry._
import unyo.Env

class GraphPanel extends Panel() {

  val visualGraph = new VisualGraph
  val graphicsContext = new GraphicsContext
  val mover = new DefaultMover

  listenTo(this.mouse.clicks, this.mouse.moves)
  var prevPoint: java.awt.Point = null
  reactions += {
    case MousePressed(_, p, _, _, _) => prevPoint = p
    case MouseReleased(_, p, _, _, _) => prevPoint = null
    case MouseDragged(_, p, _) => {
      if (prevPoint != null) {
        graphicsContext.moveBy(prevPoint - p)
        prevPoint = p
      }
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

}
