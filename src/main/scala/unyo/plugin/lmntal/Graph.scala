package unyo.plugin.lmntal

trait Edge {
  def node: Atom
}

import collection.mutable.Set

class Mem(
  val id: Int,
  val name: String,
  val parent: Mem,
  val atoms: Set[Atom],
  val mems: Set[Mem]
) {
  def allAtoms: Set[Atom] = atoms ++ mems.flatMap(_.allAtoms)
  def allMems: Set[Mem] = mems ++ mems.flatMap(_.allMems)
}
class Atom(val id: Int, val name: String, val parent: Mem, val arity: Int, val edges: Array[Edge]) {
  def buddyAt(i: Int): Atom = {
    val buddy = actualBuddyAt(i)
    if (buddy.isProxy) buddy.actualBuddyAt(0).buddyAt(1) else buddy
  }
  def actualBuddyAt(i: Int): Atom = edges(i).node
  def isProxy = name == "$in" || name == "$out"
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
    val rootMem = buildMem(parse(s), null)
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
              val node = new Atom(nextID, data, mem, 1, Array(Link(n)))
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

  private def buildMem(json: JValue, parent: Mem): Mem = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"
    val mem = new Mem(id.toInt, name, parent, Set(), Set())
    mem.atoms ++= atoms.map(buildAtom(_, mem))
    mem.mems ++= mems.map(buildMem(_, mem))
    mem
  }

  private def buildAtom(json: JValue, parent: Mem): Atom = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)
    new Atom(id.toInt, name, parent, links.size, links.map(buildEdge _).toArray)
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


import unyo.util._

class ViewContext {
  private val viewNodeFromID = collection.mutable.Map.empty[Int, View]
  val r = new util.Random
  def viewOf(node: Atom): View = {
    viewNodeFromID.getOrElseUpdate(node.id, {
      new View(Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(40, 40)))
    })
  }
  def viewOf(graph: Mem): View = {
    viewNodeFromID.getOrElseUpdate(graph.id, {
      new View(coverableRect(graph))
    })
  }
  def coverableRect(g: Mem): Rect = {
    val rects = g.mems.map(viewOf(_).rect) ++ g.atoms.filter(!_.isProxy).map(viewOf(_).rect)
    if (rects.isEmpty) Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(80, 80))
    else               rects.reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
  }

  var rootMem: Mem = null
  def rewrite(g: Mem) {
    rootMem = g
    updateMem(rootMem)
  }
  private def updateMem(g: Mem) {
    for (node <- g.atoms) viewOf(node)
    for (graph <- g.mems) updateMem(graph)
  }

  def viewOptAt(wp: Point): Option[View] = {
    rootMem.allAtoms.map(viewOf(_)).find(_.rect.contains(wp))
  }
}

class View(var rect: Rect) {
  var speed = Point(0, 0)

  val mass = 10.0
  val decayRate = 0.90
  def force(f: Point, elapsed: Double) {
    speed = (speed + f * elapsed / mass) * decayRate
    rect = Rect(rect.point + speed * elapsed, rect.dim)
  }
}
