package unyo.plugin.lmntal


import unyo.utility._
import unyo.utility.model._

class ViewContext {
  private val viewNodeFromID = collection.mutable.Map.empty[ID, View]

  lazy val gctx = unyo.gui.MainFrame.instance.mainPanel.graphicsContext
  private def initView(node: Node): Rect = {
    node.attribute match {
      case Atom()   => Rect(Point.randomPointIn(gctx.wRect), Dim(24, 24))
      case HLAtom() => Rect(Point.randomPointIn(gctx.wRect), Dim(12, 12))
      case Mem()    => coverableRect(node)
      case _        => Rect(Point(0, 0), Dim(0, 0))
    }
  }

  def viewOf(node: Node): View = viewNodeFromID.getOrElseUpdate(node.id, new View(initView(node)))

  def coverableRect(g: Node): Rect = {
    val rects = g.childNodes.map(viewOf(_).rect)
    if (rects.isEmpty) Rect(Point(Random.double * 800, Random.double * 800), Dim(80, 80))
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
    graph.rootNode.allChildNodes.filter(_.childNodes.isEmpty).map(viewOf(_)).find(_.rect.contains(wp))
  }
}

class View(var rect: Rect) {
  var speed = Point(0, 0)

  val mass = 0.1
  val decayRate = 0.90
  def gainForce(f: Point, elapsed: Double) {
    speed = (speed + f * elapsed / mass) * decayRate
    rect = Rect(rect.point + speed * elapsed, rect.dim)
  }
  def gainSpeed(s: Point, elapsed: Double) {
    rect = Rect(rect.point + s * elapsed, rect.dim)
  }

  override def toString = "View(rect: " + rect + ", speed: " + speed + ")"
}
