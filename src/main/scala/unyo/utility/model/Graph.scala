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


class EdgeBuilder(var sourceID: ID, var sourcePos: Int, var targetID: ID, var targetPos: Int, b: Builder) {
  def sourceNode = b.nodeOf(sourceID)
  def targetNode = b.nodeOf(targetID)

  def reverseEdge = targetNode.edges(targetPos)

  def build = Builder.EdgeImpl(sourceID, sourcePos, targetID, targetPos)
}

class NodeBuilder(var id: ID, var name: String, val parent: NodeBuilder, b: Builder) {

  var attribute: Attr = _

  def childNodes = b.childNodesOf(id)
  def neighborNodes = edges.map(_.targetNode)
  def edges = b.edgesOf(id)

  def addChildNode(id: ID, name: String): NodeBuilder = b.addNode(id, name, this)
  def addEdge(pos: Int, targetID: ID, targetPos: Int) = b.addEdge(id, pos, targetID, targetPos)

  def build: Builder.NodeImpl = {
    val node = Builder.NodeImpl(id, name, edges.map(_.build), childNodes.map(_.build), attribute)
    for (n <- node.childNodes) n.parent = node
    node
  }
}

class Builder {

  import collection.mutable.{ArrayBuffer => Buffer, Map}
  import unyo.utility.Tapper._

  private val nodeFromID = Map.empty[ID,NodeBuilder]
  private val nodesFromParentID = Map.empty[ID,Buffer[NodeBuilder]]
  private val edgesFromID = Map.empty[ID,Buffer[EdgeBuilder]]

  private var _root: NodeBuilder = _
  def root = _root

  private def createNode(id: ID, name: String, parent: NodeBuilder) = new NodeBuilder(id, name, parent, this).tap { n => nodeFromID += n.id -> n }

  def nodeOf(id: ID) = nodeFromID(id)
  def childNodesOf(id: ID) = nodesFromParentID.getOrElseUpdate(id, Buffer.empty[NodeBuilder])
  def edgesOf(id: ID) = edgesFromID.getOrElseUpdate(id, Buffer.empty[EdgeBuilder])

  def addRoot(id: ID, name: String) = createNode(id, name, null).tap { _root = _ }
  def addNode(id: ID, name: String, parent: NodeBuilder) = createNode(id, name, parent).tap { childNodesOf(parent.id) += _ }
  def addEdge(sid: ID, spos: Int, tid: ID, tpos: Int) = new EdgeBuilder(sid, spos, tid, tpos, this).tap { edgesOf(sid) += _ }

  def removeNode(id: ID) {
    val node = nodeOf(id)
    nodeFromID.remove(id)
    childNodesOf(node.parent.id) -= node
    edgesFromID.remove(id)
  }

  def build: Graph = {
    val graph = Builder.GraphImpl(_root.build)
    val concreteNodeFromID = graph.allNodes.map { n => (n.id, n) }.toMap
    for (node <- graph.allNodes; edge <- node.edges) {
      edge.sourceNode = concreteNodeFromID(edge.sourceID)
      edge.targetNode = concreteNodeFromID(edge.targetID)
    }
    graph
  }
}

