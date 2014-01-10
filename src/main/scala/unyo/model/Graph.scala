package unyo.model

import unyo.util._

import collection.mutable.{ArrayBuffer,Map}

trait ID
trait Attr

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
}

object Port {
  def apply(id: ID, pos: Int) = new Port(id, pos)
}

object Edge {
  def apply(source: Port, target: Port) = new Edge(source, target)
}

object Node {
  def apply(id: ID, name: String, attr: Attr = null) = new Node(id, name, attr)
}

class Port(var id: ID, var pos: Int)

class Edge(var source: Port, var target: Port)

class Graph(var rootNode: Node) {
  private val viewFromID = Map.empty[ID,View]
  private val nodeFromID = Map.empty[ID,Node]

  rootNode.graph = this
  register(rootNode)

  var viewBuilder = (n: Node) => new View(Rect(Point.zero, Dim(20, 20)), Color.BLACK)

  def register(node: Node) = nodeFromID += node.id -> node
  def unregister(node: Node) = nodeFromID -= node.id

  def viewOf(node: Node): View = viewFromID.getOrElseUpdate(node.id, viewBuilder(node))
  def nodeOf(id: ID): Node = nodeFromID(id)

  def allNodes: Seq[Node] = rootNode.allChildNodes :+ rootNode

  def inheritViews(oldGraph: Graph): Graph = {
    viewFromID ++= oldGraph.viewFromID
    this
  }
}

class Node(val id: ID, var name: String, var attr: Attr = null) {
  var parent: Node = _
  var graph: Graph = _

  val childNodes = ArrayBuffer.empty[Node]
  val edges = ArrayBuffer.empty[Edge]

  def arity = edges.size

  def addChildNode(n: Node): Node = { childNodes += n; n.graph = graph; n.parent = this; graph.register(n); this }
  def removeChildNode(n: Node): Node = { childNodes -= n; graph.unregister(n); this }
  def addEdgeTo(p: Port): Node = { edges += new Edge(new Port(id, edges.size), p); this }
  def removeFromParent = parent.removeChildNode(this)

  def neighborNodeAt(pos: Int) = graph.nodeOf(edges(pos).target.id)
  def neighborNodes: Seq[Node] = edges.map { e => graph.nodeOf(e.target.id) }
  def allChildNodes: Seq[Node] = childNodes ++ childNodes.flatMap { _.allChildNodes }
  def isRoot = parent == null

  def view = graph.viewOf(this)

  override def toString = s"Node(${id}, ${name}, ${arity}, ${attr})"
}
