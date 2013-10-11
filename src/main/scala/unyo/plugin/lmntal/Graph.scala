package unyo.plugin.lmntal

trait Edge {
  def node: Atom
}

import collection.mutable.Set

case class Mem(id: Int, name: String, atoms: Set[Atom], mems: Set[Mem])
case class Atom(id: Int, name: String, arity: Int, edges: Array[Edge]) {
  def buddyAt(i: Int): Atom = edges(i).node
}
case class Raw(attr: Int, data: String) extends Edge {
  def node: Atom = throw new RuntimeException("Raw link exists")
}
case class Link(node: Atom) extends Edge


object Mem {
  import org.json4s._
  import org.json4s.native.JsonMethods._
  import collection.mutable.ArrayBuffer

  def fromString(s: String): Mem = {
    val rootMem = buildMem(parse(s))
    val nodeMap = atomMapIn(rootMem)

    configureLinks(rootMem, nodeMap)

    rootMem
  }

  var idSeed = 65535
  def nextID = { idSeed += 1; idSeed }

  private def configureLinks(mem: Mem, map: Map[Int, Atom]) {
    val newAtoms = ArrayBuffer.empty[Atom]
    for (n <- mem.atoms) {
      val edges = n.edges
      for (i <- 0 until edges.size) {
        edges(i) match {
          case Raw(attr, data) => {
            if ((attr & 0x80) == 0) {
              edges(i) = Link(map(data.toInt))
            } else {
              val node = Atom(nextID, data, 1, Array(Link(n)))
              edges(i) = Link(node)
              newAtoms += node
            }
          }
          case _ =>
        }
      }
    }
    for (g <- mem.mems) configureLinks(g, map)
    mem.atoms ++= newAtoms
  }

  private def atomMapIn(mem: Mem): Map[Int, Atom] = {
    val b = Map.newBuilder[Int, Atom]

    for (n <- mem.atoms) b += n.id -> n
    for (n <- mem.mems) b ++= atomMapIn(n)

    b.result
  }

  private def buildMem(json: JValue): Mem = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"
    Mem(
      id.toInt,
      name,
      Set() ++ atoms.map(buildAtom _),
      Set() ++ mems.map(buildMem _)
    )
  }

  private def buildAtom(json: JValue): Atom = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)
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
