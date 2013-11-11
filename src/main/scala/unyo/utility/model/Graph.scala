package unyo.utility.model

trait ID
trait Attr

trait Edge {
  def sourceNode: Node
  def targetNode: Node
  def sourcePos: Int
  def targetPos: Int
}

trait Node {
  def id: ID
  def name: String
  def edges: Seq[Edge]
  def neighborNodes: Seq[Node]
  def childNodes: Seq[Node]
  def attribute: Attr
}

trait Graph {
  def rootNode: Node
}

object Builder {

  import collection.mutable.{ArrayBuffer,Map}

  case class MutableNode(val id: ID, var name: String)

  case class NodeImpl(id: ID, name: String, edges: Seq[Edge], childNodes: Seq[Node]) extends Node {
    def addNode(node: Node) {}
    def neighborNodes: Seq[Node] = null
    def attribute: Attr = null
  }

  case class Port(id: ID, pos: Int)

  case class EdgeImpl(source: Port, target: Port) extends Edge {
    def sourcePos = source.pos
    def targetPos = target.pos
    var sourceNode: Node = null
    var targetNode: Node = null
  }

  case class GraphImpl(rootNode: Node) extends Graph
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
    graph
  }

  private def buildNode(mnode: MutableNode): Node = {
    val edges = edgesFromID.getOrElse(mnode.id, Seq.empty[EdgeImpl]).sortWith(_.source.pos < _.target.pos)
    val childNodes = nodesFromParentID.getOrElse(mnode.id, ArrayBuffer.empty[MutableNode]).map(buildNode _)

    NodeImpl(mnode.id, mnode.name, edges, childNodes)
  }

  private def allChildNodesOf(node: Node): Seq[Node] = node.childNodes ++ node.childNodes.flatMap(allChildNodesOf(_))

}
