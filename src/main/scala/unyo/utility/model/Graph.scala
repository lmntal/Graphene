package unyo.utility.model

trait ID
trait Attr

trait Edge {
  def sourceNode: Node
  def targetNode: Node
  def sourcePos: Int
  def targetPos: Int
}

abstract class Node {
  def id: ID
  def name: String
  def parent: Node
  def edges: Seq[Edge]
  def childNodes: Seq[Node]
  def attribute: Attr

  def isRoot = parent == null
  def neighborNodes = edges.map(_.targetNode)
  def allChildNodes: Seq[Node] = childNodes ++ childNodes.flatMap(_.allChildNodes)
}

trait Graph {
  def rootNode: Node
  def allNodes: Seq[Node]
}

object Builder {

  import collection.mutable.{ArrayBuffer,Map}

  case class MutableNode(val id: ID, var name: String) {
    var attribute: Attr = null
  }

  case class NodeImpl(id: ID, name: String, edges: Seq[Edge], childNodes: Seq[Node]) extends Node {
    var parent: Node = null
    var attribute: Attr = null
    def addNode(node: Node) {}
    override val allChildNodes: Seq[Node] = childNodes ++ childNodes.flatMap(_.allChildNodes)
  }

  case class Port(id: ID, pos: Int)

  case class EdgeImpl(source: Port, target: Port) extends Edge {
    def sourcePos = source.pos
    def targetPos = target.pos
    var sourceNode: Node = null
    var targetNode: Node = null
  }

  case class GraphImpl(rootNode: Node) extends Graph {
    def allNodes: Seq[Node] = rootNode.allChildNodes :+ rootNode
    override def toString = {
      def nodeToString(sb: StringBuilder, node: Node, depth: Int) {
        sb ++= " " * depth
        sb ++= s"Node(${node.id}, ${node.name})\n"
        for (n <- node.childNodes) nodeToString(sb, n, depth + 1)
      }
      val sb = new StringBuilder
      sb ++= "Graph(\n"
      nodeToString(sb, rootNode, 1)
      sb ++= ")"
      sb.toString
    }
  }
}

class Builder {

  import collection.mutable.{ArrayBuffer,Map}
  import unyo.utility.Tapper._
  import Builder._

  private val nodeFromID = Map.empty[ID, MutableNode]
  private val nodesFromParentID = Map.empty[ID, ArrayBuffer[MutableNode]]
  private val edgesFromID = Map.empty[ID, ArrayBuffer[EdgeImpl]]
  private var root: MutableNode = null

  private def createNode(id: ID, name: String) = MutableNode(id, name).tap { n => nodeFromID += n.id -> n }

  def addRoot(id: ID, name: String): MutableNode = createNode(id, name).tap { root = _ }

  def addNode(id: ID, name: String, parent: MutableNode) =
    createNode(id, name).tap { nodesFromParentID.getOrElseUpdate(parent.id, ArrayBuffer.empty[MutableNode]) += _ }

  def addEdge(s: ID, sp: Int, t: ID, tp: Int) =
    edgesFromID.getOrElseUpdate(s, ArrayBuffer.empty[EdgeImpl]) += EdgeImpl(Port(s, sp), Port(t, tp))

  def build: Graph = {
    val graph = GraphImpl(buildNode(root))
    val concreteNodeFromID = graph.allNodes.map { n => (n.id, n) }.toMap
    for (edge <- edgesFromID.values.flatten) {
      edge.sourceNode = concreteNodeFromID(edge.source.id)
      edge.targetNode = concreteNodeFromID(edge.target.id)
    }
    graph
  }

  private def buildNode(mnode: MutableNode): NodeImpl = {
    val edges = edgesFromID.getOrElse(mnode.id, Seq.empty[EdgeImpl]).sortWith(_.source.pos < _.target.pos)
    val childNodes = nodesFromParentID.getOrElse(mnode.id, ArrayBuffer.empty[MutableNode]).map(buildNode _)

    val node = NodeImpl(mnode.id, mnode.name, edges, childNodes)
    node.attribute = mnode.attribute
    for (n <- childNodes) n.parent = node
    node
  }

  private def allChildNodesOf(node: Node): Seq[Node] = node.childNodes ++ node.childNodes.flatMap(allChildNodesOf(_))

}
