package graphene.model

import graphene.util._
import graphene.util.Tapper._


import collection.mutable.{ArrayBuffer,Map}

trait ID
trait Attr
object NoAttr extends Attr

import java.awt.{Color}

class View(var rect: Rect, var color: Color) {

  def speed = Point(sx, sy)
  var sx = 0.0 // speed x
  var sy = 0.0 // speed y
  var dx = 0.0
  var dy = 0.0
  var fixed = false
  var selected = false
  var didAppear = false
  var willDisappear = false

  val mass = 0.1
  val decayRate = 0.90

  def reset() {
    dx = 0.0
    dy = 0.0
  }
  def affect(f: Point, elapsedSec: Double) {
    sx = sx * decayRate + f.x / mass * elapsedSec
    sy = sy * decayRate + f.y / mass * elapsedSec
    dx = sx * elapsedSec
    dy = sy * elapsedSec
  }
  def move() {
    val abs = math.hypot(dx, dy)
    // TODO: Magic number
    if (abs < 100) {
      rect = Rect(Point(rect.point.x + dx, rect.point.y + dy), rect.dim)
    } else {
      rect = Rect(Point(rect.point.x + dx / abs * 100, rect.point.y + dy / abs * 100), rect.dim)
    }
  }

  override def toString = "View(rect: " + rect + ", speed: " + s"($sx, $sy)" + ")"

  def deepcopy = {
    var v = new View(rect, color)

    v.sx = sx
    v.sy = sy
    v.dx = dx
    v.dy = dy
    v.fixed = fixed
    v.selected = selected
    v.didAppear = didAppear
    v.willDisappear = willDisappear

    v
  }
}

object Edge {
  def apply(source: Node, target: Node) = new Edge(source, target)
}

object Node {
  def apply(graph: Graph, parent: Node, id: ID, name: String, attr: Attr) = new Node(graph, parent, id, name, attr)
}

class Edge(val source: Node, val target: Node) {
  def adjacentNodeOf(n: Node) =
    if      (n.id == source.id) target
    else if (n.id == target.id) source
    else throw new Exception(s"$n is not member of $this")
}

class Graph {

  private val viewFromID = Map.empty[ID,View]
  private val nodeFromID = Map.empty[ID,Node]

  var rootNode: Node = _
  var viewBuilder = (n: Node) => new View(Rect(Point.zero, Dim(20, 20)), Color.BLACK)

  val allEdges = ArrayBuffer.empty[Edge]

  def createRootNode(id: ID, name: String, attr: Attr = NoAttr) =
    Node(this, null, id, name, attr).tap { n => rootNode = n; register(n) }

  def createEdge(source: ID, target: ID): Edge = createEdge(nodeFromID(source), nodeFromID(target))

  def createEdge(source: Node, target: Node) =
    Edge(source, target).tap { e => allEdges += e; source.addEdge(e); target.addEdge(e) }

  def removeEdge(e: Edge) = { allEdges -= e; e.source.removeEdge(e); e.target.removeEdge(e) }

  private[model] def register(node: Node) = nodeFromID += node.id -> node
  private[model] def unregister(node: Node) = nodeFromID -= node.id

  def viewOf(node: Node): View = viewFromID.getOrElseUpdate(node.id, viewBuilder(node))
  def nodeOf(id: ID): Node = nodeFromID(id)

  def allNodes: Seq[Node] = rootNode.allChildNodes :+ rootNode

  def inheritViews(oldGraph: Graph): Graph = {
    viewFromID ++= oldGraph.viewFromID
    this
  }

  def deepcopy = {
    def copyNode(parent: Node, src: Node): Node = {
      val dst = parent.createNode(src.id, src.name, src.attr)
      for (n <- src.childNodes) copyNode(dst, n)
      dst
    }
    def copyRootNode(dstGraph: Graph, srcGraph: Graph): Node = {
      val dst = dstGraph.createRootNode(srcGraph.rootNode.id, srcGraph.rootNode.name, srcGraph.rootNode.attr)
      for (n <- srcGraph.rootNode.childNodes) copyNode(dst, n)
      dst
    }
    val g = new Graph
    g.viewBuilder = viewBuilder
    for ((id, view) <- viewFromID) g.viewFromID += id -> view.deepcopy

    val root = copyRootNode(g, this)

    for (e <- allEdges) g.createEdge(e.source.id, e.target.id)

    g
  }
}

class Node private(val graph: Graph, val parent: Node, val id: ID, var name: String, var attr: Attr) {

  val childNodes = ArrayBuffer.empty[Node]
  val neighborNodes = ArrayBuffer.empty[Node]
  val edges = ArrayBuffer.empty[Edge]

  def arity = neighborNodes.size

  def createNode(id: ID, name: String, attr: Attr = NoAttr) =
    Node(graph, this, id, name, attr).tap { n => childNodes += n; graph.register(n) }

  def removeChildNode(n: Node) = { childNodes -= n; graph.unregister(n) }
  def removeFromParent = parent.removeChildNode(this)

  def neighborNodeAt(pos: Int) = neighborNodes(pos)
  def allChildNodes: Seq[Node] = childNodes ++ childNodes.flatMap { _.allChildNodes }
  def isRoot = parent == null

  def view = graph.viewOf(this)

  private[model] def addEdge(e: Edge) = { edges += e; neighborNodes += e.adjacentNodeOf(this) }
  private[model] def removeEdge(e: Edge) = { edges -= e; neighborNodes -= e.adjacentNodeOf(this) }

  override def toString = s"Node(${id}, ${name}, ${arity}, ${attr})"
}
