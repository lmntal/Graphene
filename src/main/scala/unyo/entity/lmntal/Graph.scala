package unyo.entity.lmntal

import unyo.entity.{Graph, Node, Edge}

import collection.mutable.Set

case class Membrane(id: Int, name: String, nodes: Set[Atom], graphs: Set[Membrane]) extends Graph
case class Atom(id: Int, name: String, arity: Int, edges: Array[Edge]) extends Node {
  def buddyAt(i: Int): Node = edges(i).node
}
case class Raw(attr: Int, data: String) extends Edge {
  def node: Node = throw new RuntimeException("Raw link exists")
}
case class Link(node: Atom) extends Edge


object Membrane {
  import org.json4s._
  import org.json4s.native.JsonMethods._
  import collection.mutable.ArrayBuffer

  def fromString(s: String): Membrane = {
    val graph = buildMembrane(parse(s))
    val nodeMap = atomMapOf(graph)

    configureLinks(graph, nodeMap)

    graph
  }

  var idSeed = 65535
  def nextID = { idSeed += 1; idSeed }

  private def configureLinks(graph: Membrane, map: Map[Int, Atom]) {
    val newNodes = ArrayBuffer.empty[Atom]
    for (n <- graph.nodes) {
      val edges = n.edges
      for (i <- 0 until edges.size) {
        edges(i) match {
          case Raw(attr, data) => {
            if ((attr & 0x80) == 0) {
              edges(i) = Link(map(data.toInt))
            } else {
              val node = Atom(nextID, data, 1, Array(Link(n)))
              edges(i) = Link(node)
              newNodes += node
            }
          }
          case _ =>
        }
      }
    }
    for (g <- graph.graphs) configureLinks(g, map)
    graph.nodes ++= newNodes
  }

  private def atomMapOf(graph: Membrane): Map[Int, Atom] = {
    val b = Map.newBuilder[Int, Atom]

    for (n <- graph.nodes) b += n.id -> n
    for (n <- graph.graphs) b ++= atomMapOf(n)

    b.result
  }

  private def buildMembrane(json: JValue): Membrane = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(nodes) = json \ "atoms"
    val JArray(graphs) = json \ "membranes"
    Membrane(
      id.toInt,
      name,
      Set() ++ nodes.map(buildAtom _),
      Set() ++ graphs.map(buildMembrane _)
    )
  }

  private def buildAtom(json: JValue): Atom = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(links) = json \ "links"
    Atom(id.toInt, name, links.size, links.map(buildEdge _).toArray)
  }

  private[this] val intAttr      = 0x80 | 0x00
  private[this] val dblAttr      = 0x80 | 0x01
  private[this] val strAttr      = 0x80 | 0x03
  private[this] val constStrAttr = 0x80 | 0x04
  private[this] val constDblAttr = 0x80 | 0x05
  private[this] val constHLAttr  = 0x80 | 0x0a
  private def buildEdge(json: JValue): Edge = {
    val JInt(attr) = json \ "attr"
    val data = (json \ "data") match {
      case JString(s) => s
      case JDouble(d) => d.toString
      case JInt(i)    => i.toString
      case j          => throw new Exception("Unexpected data : " + j.toString)
    }
    Raw(attr.toInt, data)
  }
}
