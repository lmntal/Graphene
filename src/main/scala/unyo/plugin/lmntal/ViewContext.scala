package unyo.plugin.lmntal


import unyo.utility._
import unyo.utility.model._

class ViewContext {
  private val viewNodeFromID = collection.mutable.Map.empty[ID, View]

  lazy val config = LMNtalPlugin.config

  lazy val gctx = unyo.gui.MainFrame.instance.mainPanel.graphicsContext
  private def initView(node: Node): Rect = {
    node.attribute match {
      case Atom()   => Rect(Point.randomPointIn(gctx.wRect), Dim(24, 24))
      case HLAtom() => Rect(Point.randomPointIn(gctx.wRect), Dim(12, 12))
      case Mem()    => coverableRect(node)
      case _        => Rect(Point(0, 0), Dim(0, 0))
    }
  }

  def viewOf(id: ID): View = viewNodeFromID(id)
  def viewOf(node: Node): View = viewNodeFromID.getOrElseUpdate(node.id, new View(node, initView(node)))

  def coverableRect(g: Node): Rect = {
    val rects = g.childNodes.map(viewOf(_).rect)
    if (rects.isEmpty) Rect(Point(Random.double * 800, Random.double * 800), Dim(80, 80))
    else               rects.reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
  }

  private def playDiffAnimation(g: Graph) {
    val newNodes = {
      val oldIDs = graph.allNodes.map(_.id).toSet
      for (n <- g.allNodes if !oldIDs.contains(n.id)) yield n
    }
    val oldNodes = {
      val newIDs = g.allNodes.map(_.id).toSet
      for (n <- graph.allNodes if !newIDs.contains(n.id)) yield n
    }

    for (n <- oldNodes) viewOf(n).willDisappear = true

    actors.Actor.actor {
      Thread.sleep(1000)
      for (n <- oldNodes) viewOf(n).willDisappear = false

      graph = g
      updateGraph(graph)

      for (n <- newNodes) viewOf(n).didAppear = true
      actors.Actor.actor {
        Thread.sleep(1500)
        for (n <- newNodes) viewOf(n).didAppear = false
      }
    }
  }

  var graph: Graph = null
  def rewrite(g: Graph) {
    if (graph != null && config.isDiffAnimationEnabled) {
      playDiffAnimation(g)
    } else {
      graph = g
      updateGraph(graph)
    }
  }
  private def updateGraph(graph: Graph) = updateNode(graph.rootNode)
  private def updateNode(node: Node) {
    for (n <- node.childNodes) viewOf(n)
    for (n <- node.childNodes) updateNode(n)
  }

  def viewOptAt(wp: Point): Option[View] = {
    graph.rootNode.allChildNodes.filter(_.childNodes.isEmpty).map(viewOf(_)).find(_.rect.contains(wp))
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
  var fixed = false
  var didAppear = false
  var willDisappear = false

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
