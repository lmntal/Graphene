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

  class MutableNode(val id: ID, var name: String, var attribute: Attr = null)

  class MutableEdge(var sourceID: ID, var sourcePos: Int, var targetID: ID, var targetPos: Int)

  case class NodeImpl(id: ID, name: String, edges: Seq[EdgeImpl], childNodes: Seq[NodeImpl], attribute: Attr) extends Node {
    var parent: NodeImpl = null
    override val allChildNodes: Seq[NodeImpl] = childNodes ++ childNodes.flatMap(_.allChildNodes)
  }

  case class EdgeImpl(sourceID: ID, sourcePos: Int, targetID: ID, targetPos: Int) extends Edge {
    var sourceNode: Node = null
    var targetNode: Node = null
  }

  case class GraphImpl(rootNode: NodeImpl) extends Graph {
    def allNodes: Seq[NodeImpl] = rootNode.allChildNodes :+ rootNode
    override def toString = {
      def nodeToString(sb: StringBuilder, node: NodeImpl, depth: Int) {
        sb.append(" " * depth).append(s"Node(${node.id}, ${node.name})\n")
        for (n <- node.childNodes) nodeToString(sb, n, depth + 1)
      }
      val sb = new StringBuilder("Graph(\n")
      nodeToString(sb, rootNode, 1)
      sb.append(")").toString
    }
  }
}

class Builder {

  import collection.mutable.{ArrayBuffer => Buffer,Map}
  import unyo.utility.Tapper._
  import Builder._

  private val nodeFromID = Map.empty[ID, MutableNode]
  private val nodesFromParentID = Map.empty[ID, Buffer[MutableNode]]
  private val edgesFromID = Map.empty[ID, Buffer[MutableEdge]]
  private var root: MutableNode = null

  private def createNode(id: ID, name: String) = new MutableNode(id, name).tap { n => nodeFromID += n.id -> n }

  def addRoot(id: ID, name: String): MutableNode = createNode(id, name).tap { root = _ }

  def addNode(id: ID, name: String, parent: MutableNode) =
    createNode(id, name).tap { nodesFromParentID.getOrElseUpdate(parent.id, Buffer.empty[MutableNode]) += _ }

  def addEdge(sid: ID, sp: Int, tid: ID, tp: Int) =
    edgesFromID.getOrElseUpdate(sid, Buffer.empty[MutableEdge]) += new MutableEdge(sid, sp, tid, tp)

  def build: Graph = {
    val graph = GraphImpl(buildNode(root))
    val concreteNodeFromID = graph.allNodes.map { n => (n.id, n) }.toMap
    for (node <- graph.allNodes; edge <- node.edges) {
      edge.sourceNode = concreteNodeFromID(edge.sourceID)
      edge.targetNode = concreteNodeFromID(edge.targetID)
    }
    graph
  }

  private def buildNode(mnode: MutableNode): NodeImpl = {
    val edges = edgesFromID.getOrElse(mnode.id, Seq.empty[MutableEdge]).map { e =>
      EdgeImpl(e.sourceID, e.sourcePos, e.targetID, e.targetPos)
    }.sortWith(_.sourcePos < _.targetPos)
    val childNodes = nodesFromParentID.getOrElse(mnode.id, Buffer.empty[MutableNode]).map(buildNode _)

    val node = NodeImpl(mnode.id, mnode.name, edges, childNodes, mnode.attribute)
    for (n <- childNodes) n.parent = node
    node
  }

  private def allChildNodesOf(node: Node): Seq[Node] = node.childNodes ++ node.childNodes.flatMap(allChildNodesOf(_))

}
