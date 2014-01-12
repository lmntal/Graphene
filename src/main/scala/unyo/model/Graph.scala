package unyo.model

import unyo.util._
import unyo.util.Tapper._


import collection.mutable.{ArrayBuffer,Map}

trait ID
trait Attr
case class NoAttr()

import java.awt.{Color}

class View(var rect: Rect, var color: Color) {

  var speed = Point.zero
  var diff = Point.zero
  var fixed = false
  var selected = false
  var didAppear = false
  var willDisappear = false

  val mass = 0.1
  val decayRate = 0.90

  def reset() {
    diff = Point.zero
  }
  def affect(s: Point, f: Point, elapsedSec: Double) {
    speed = speed * decayRate + f / mass * elapsedSec
    diff = (speed + s) * elapsedSec
  }
  def move() {
    val abs = diff.abs
    // TODO: Magic number
    if (abs < 100) {
      rect = Rect(rect.point + diff, rect.dim)
    } else {
      rect = Rect(rect.point + diff.unit * 100, rect.dim)
    }
  }

  override def toString = "View(rect: " + rect + ", speed: " + speed + ")"
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
  val allEdges = ArrayBuffer.empty[Edge]

  def createRootNode(id: ID, name: String, attr: Attr) =
    Node(this, null, id, name, attr).tap { n => rootNode = n; register(n) }

  def createEdge(source: ID, target: ID): Edge = createEdge(nodeFromID(source), nodeFromID(target))

  def createEdge(source: Node, target: Node) =
    Edge(source, target).tap { e => allEdges += e; source.addEdge(e); target.addEdge(e) }

  def removeEdge(e: Edge) = { allEdges -= e; e.source.removeEdge(e); e.target.removeEdge(e) }

  var viewBuilder = (n: Node) => new View(Rect(Point.zero, Dim(20, 20)), Color.BLACK)

  private[model] def register(node: Node) = nodeFromID += node.id -> node
  private[model] def unregister(node: Node) = nodeFromID -= node.id

  def viewOf(node: Node): View = viewFromID.getOrElseUpdate(node.id, viewBuilder(node))
  def nodeOf(id: ID): Node = nodeFromID(id)

  def allNodes: Seq[Node] = rootNode.allChildNodes :+ rootNode

  def inheritViews(oldGraph: Graph): Graph = {
    viewFromID ++= oldGraph.viewFromID
    this
  }
}

class Node private(val graph: Graph, val parent: Node, val id: ID, var name: String, var attr: Attr) {

  val childNodes = ArrayBuffer.empty[Node]
  val neighborNodes = ArrayBuffer.empty[Node]
  val edges = ArrayBuffer.empty[Edge]

  def arity = neighborNodes.size

  def createNode(id: ID, name: String, attr: Attr) =
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
