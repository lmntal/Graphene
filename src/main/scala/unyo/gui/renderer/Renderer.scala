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


class DefaultRenderer(val g: Graphics, val context: GraphicsContext) extends Renderer {

  var visualGraph: VisualGraph = null
  def render(graph: VisualGraph) {
    visualGraph = graph
    g.clearRect(0, 0, 2000, 2000)
    renderRoot(graph.graph)
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
    g.fillOval(viewNode.pos - Point(20, 20), Dimension(40, 40))
    g.setColor(Color.WHITE)
    g.fillOval(viewNode.pos - Point(17, 17), Dimension(34, 34))
  }

  def renderEdges(node: Node) {
    val view1 = visualGraph.viewNodeOf(node)
    g.setColor(new Color(52, 73, 94))
    for (i <- 0 until node.arity) {
      val buddy = node.buddyAt(i)
      val view2 = visualGraph.viewNodeOf(buddy)

      g.drawLine(view1.pos, view2.pos)
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

class GraphicsContext {
  var center = Point(0, 0)
  var size = Dimension(800, 600)
}
