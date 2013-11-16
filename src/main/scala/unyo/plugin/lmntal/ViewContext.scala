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

  def viewOf(node: Node): View = viewNodeFromID.getOrElseUpdate(node.id, new View(node, initView(node)))
  def isProxy(node: Node) = node.name == "$in" || node.name == "$out"
  def actualNode(node: Node): Node = if (isProxy(node)) actualNode(node.neighborNodes(1).neighborNodes(0)) else node
  def neighborNodesOf(node: Node) = node.neighborNodes.map(actualNode(_))
  def childNodesOf(node: Node) = node.childNodes.filter(!isProxy(_))
  def allChildNodesOf(node: Node) = node.allChildNodes.filter(!isProxy(_))

  def coverableRect(g: Node): Rect = {
    val rects = childNodesOf(g).map(viewOf(_).rect)
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
    for (n <- childNodesOf(node)) viewOf(n)
    for (n <- childNodesOf(node)) updateNode(n)
  }

  def viewOptAt(wp: Point): Option[View] = {
    graph.rootNode.allChildNodes.filter(childNodesOf(_).isEmpty).map(viewOf(_)).find(_.rect.contains(wp))
  }

  def transaction(f: => Unit) {
    for ((_, view) <- viewNodeFromID) view.reset
    f
    for ((_, view) <- viewNodeFromID) view.move
  }
}

class View(node: Node, var rect: Rect) {
  var speed = Point(0, 0)
  var diff = Point(0, 0)

  val mass = 0.1
  val decayRate = 0.90
  def reset() {
    diff = Point(0, 0)
  }
  def affect(s: Point, f: Point, elapsedSec: Double) {
    speed = speed * decayRate + f / mass * elapsedSec
    diff = (speed + s) * elapsedSec
  }
  def move() {
    rect = Rect(rect.point + diff, rect.dim)
  }

  override def toString = "View(rect: " + rect + ", speed: " + speed + ")"
}
