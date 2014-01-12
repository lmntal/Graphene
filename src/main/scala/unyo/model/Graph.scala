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

object Port {
  def apply(id: ID, pos: Int) = new Port(id, pos)
}

object Edge {
  def apply(source: Port, target: Port) = new Edge(source, target)
}

object Node {
  def apply(graph: Graph, parent: Node, id: ID, name: String, attr: Attr) = new Node(graph, parent, id, name, attr)
}

class Port(var id: ID, var pos: Int)

class Edge(var source: Port, var target: Port)

class Graph {

  private val viewFromID = Map.empty[ID,View]
  private val nodeFromID = Map.empty[ID,Node]

  var rootNode: Node = _

  def createRootNode(id: ID, name: String, attr: Attr) =
    Node(this, null, id, name, attr).tap { n => rootNode = n; register(n) }

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
  val edges = ArrayBuffer.empty[Edge]

  def arity = edges.size

  def createNode(id: ID, name: String, attr: Attr) =
    Node(graph, this, id, name, attr).tap { n => childNodes += n; graph.register(n) }

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
