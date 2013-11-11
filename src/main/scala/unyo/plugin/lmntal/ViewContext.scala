package unyo.plugin.lmntal


import unyo.utility._
import unyo.utility.model._

class ViewContext {
  private val viewNodeFromID = collection.mutable.Map.empty[ID, View]
  val r = new util.Random

  def viewOf(node: Node): View = viewNodeFromID.getOrElseUpdate(node.id, {
    val rect = node.attribute match {
      case Mem() => coverableRect(node)
      case _     => Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(24, 24))
    }
    new View(rect)
  })

  def coverableRect(g: Node): Rect = {
    val rects = g.childNodes.map(viewOf(_).rect)
    if (rects.isEmpty) Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(80, 80))
    else               rects.reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
  }

  var graph: Graph = null
  def rewrite(g: Graph) {
    graph = g
    updateGraph(graph)
  }
  private def updateGraph(graph: Graph) = updateNode(graph.rootNode)
  private def updateNode(node: Node) {
    for (n <- node.childNodes) viewOf(n)
    for (n <- node.childNodes) updateNode(n)
  }

  def viewOptAt(wp: Point): Option[View] = {
    graph.rootNode.allChildNodes.map(viewOf(_)).find(_.rect.contains(wp))
  }
}

class View(var rect: Rect) {
  var speed = Point(0, 0)

  val mass = 8.0
  val decayRate = 0.90
  def force(f: Point, elapsed: Double) {
    speed = (speed + f * elapsed / mass) * decayRate
    rect = Rect(rect.point + speed * elapsed, rect.dim)
  }

  override def toString = "View(rect: " + rect + ", speed: " + speed + ")"
}
