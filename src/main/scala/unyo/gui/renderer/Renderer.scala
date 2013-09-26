package unyo.gui.renderer

import java.awt.{Graphics,Color}

import unyo.entity.{Graph,Node}
import unyo.util._

trait Renderer {
  implicit class GraphicsExt(val g: Graphics) {
    def drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
      g.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)
    }
    def drawLine(p1: Point, p2: Point) {
      drawLine(p1.x, p1.y, p2.x, p2.y)
    }

    def fillOval(x: Double, y: Double, w: Double, h: Double) {
      g.fillOval(x.toInt, y.toInt, w.toInt, h.toInt)
    }
    def fillOval(p: Point, w: Double, h: Double) {
      fillOval(p.x, p.y, w, h)
    }
    def fillOval(p: Point, d: Dimension) {
      fillOval(p, d.width, d.height)
    }
  }
}


class DefaultRenderer(val g: Graphics, val context: ViewContext) extends Renderer {

  def render(graph: Graph) {
    renderRoot(graph)
  }

  def renderRoot(graph: Graph) {
    for (node <- graph.nodes) {
      renderNode(node)
    }
  }

  def renderNode(node: Node) {
    renderEdges(node)
    val viewNode = context.viewNodeOf(node)
    g.fillOval(viewNode.pos - Point(20, 20), Dimension(40, 40))
  }

  def renderEdges(node: Node) {
    val view1 = context.viewNodeOf(node)
    for (i <- 0 until node.arity) {
      val buddy = node.buddyAt(i)
      val view2 = context.viewNodeOf(buddy)

      g.drawLine(view1.pos, view2.pos)
    }
  }
}
