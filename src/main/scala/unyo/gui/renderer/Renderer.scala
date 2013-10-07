package unyo.gui.renderer

import java.awt.{Graphics,Color}

import unyo.entity.{Graph,Node}
import unyo.util._
import unyo.util.Geometry._

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


class DefaultRenderer(val g: Graphics, val context: GraphicsContext) extends Renderer {

  var visualGraph: VisualGraph = null
  def render(graph: VisualGraph) {
    visualGraph = graph
    g.clearRect(0, 0, 2000, 2000)
    renderGrid
    renderRoot(graph.graph)
  }

  def renderGrid {
    val bx = context.wCenter.x - context.wSize.width / 2
    val ex = context.wCenter.x + context.wSize.width / 2
    val by = context.wCenter.y - context.wSize.height / 2
    val ey = context.wCenter.y + context.wSize.height / 2
    g.setColor(new Color(127, 140, 141))
    for (x <- (bx.toInt/100*100).to(ex.toInt/100*100, 100)) {
      val p1 = context.screenPointFrom(Point(x, by))
      val p2 = context.screenPointFrom(Point(x, ey))
      g.drawLine(p1, p2)
    }

    for (y <- (by.toInt/100*100).to(ey.toInt/100*100, 100)) {
      val p1 = context.screenPointFrom(Point(bx, y))
      val p2 = context.screenPointFrom(Point(ex, y))
      g.drawLine(p1, p2)
    }
  }

  def renderRoot(graph: Graph) {
    if (graph == null) return
    for (node <- graph.nodes) {
      renderEdges(node)
    }
    for (node <- graph.nodes) {
      renderNode(node)
    }
  }

  def renderNode(node: Node) {
    val viewNode = visualGraph.viewNodeOf(node)
    g.setColor(new Color(52, 152, 219))
    g.fillOval(context.screenPointFrom(viewNode.pos) - Point(20, 20), Dimension(40, 40))
    g.setColor(Color.WHITE)
    g.fillOval(context.screenPointFrom(viewNode.pos) - Point(17, 17), Dimension(34, 34))
  }

  def renderEdges(node: Node) {
    val view1 = visualGraph.viewNodeOf(node)
    g.setColor(new Color(41, 128, 185))
    for (i <- 0 until node.arity) {
      val buddy = node.buddyAt(i)
      val view2 = visualGraph.viewNodeOf(buddy)

      g.drawLine(
        context.screenPointFrom(view1.pos),
        context.screenPointFrom(view2.pos)
      )
    }
  }
}

class VisualGraph() {
  private val viewNodeFromID = collection.mutable.Map.empty[Int, VisualNode]
  val r = new util.Random
  def viewNodeOf(node: Node): VisualNode = {
    viewNodeFromID.getOrElseUpdate(node.id, new VisualNode(Point(r.nextDouble * 800, r.nextDouble * 800)))
  }

  var graph: Graph = null
  def rewrite(g: Graph) {
    graph = g
    updateGraph(g)
  }
  private def updateGraph(g: Graph) {
    for (node <- g.nodes) viewNodeOf(node)
    for (graph <- g.graphs) updateGraph(graph)
  }


}

class VisualNode(var pos: Point) {
  var speed = Point(0, 0)

  val mass = 10.0
  val decayRate = 0.90
  def force(f: Point, elapsed: Double) {
    speed = (speed + f * elapsed / mass) * decayRate
    pos = pos + speed * elapsed
  }
}

class GraphicsContext(var sSize: Dimension) {
  var wCenter = Point(sSize.width / 2, sSize.height / 2)
  var magnificationRate: Double = 1.0 // screen / world

  def wSize = Dimension(sSize.width / magnificationRate, sSize.height / magnificationRate)

  def worldPointFrom(sp: Point): Point = Point(
    wCenter.x + (sp.x - sSize.width / 2) / magnificationRate,
    wCenter.y + (sp.y - sSize.height/ 2) / magnificationRate
  )
  def screenPointFrom(wp: Point): Point = Point(
    (wp.x - wCenter.x) * magnificationRate + sSize.width / 2,
    (wp.y - wCenter.y) * magnificationRate + sSize.height/ 2
  )

  def moveBy(sv: Point) {
    wCenter = wCenter + sv / magnificationRate
  }

}
