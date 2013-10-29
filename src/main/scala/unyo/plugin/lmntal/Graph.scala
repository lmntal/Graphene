package unyo.plugin.lmntal

import collection.mutable.Set

trait ID
case class IntID(value: Int) extends ID
case class DataAtomID(id: ID, port: Int) extends ID
case class HLAtomID(value: Int) extends ID

class Mem(
  val id: ID,
  val name: String,
  val parent: Mem
) {
  val atoms = Set.empty[Atom]
  val mems = Set.empty[Mem]

  def allAtoms: Set[Atom] = atoms ++ mems.flatMap(_.allAtoms)
  def allMems: Set[Mem] = mems ++ mems.flatMap(_.allMems)
}
class Atom(
  val id: ID,
  val name: String,
  val parent: Mem,
  val links: Array[Link]
) {
  val arity = links.size
  def buddyAt(i: Int): Atom = {
    val buddy = actualBuddyAt(i)
    if (buddy.isProxy) buddy.actualBuddyAt(0).buddyAt(1) else buddy
  }
  def actualBuddyAt(i: Int): Atom = links(i).node
  def isProxy = name == "$in" || name == "$out"
}

case class Attribute(value: Int) {
  def isRef = (value & 0x80) == 0
  def isData = !isRef
  def isHL = value == 0x8a
}
trait Link {
  def node: Atom
}
case class Raw(attr: Attribute, data: String) extends Link {
  def node: Atom = throw new RuntimeException("Raw link exists")
}
case class Ref(node: Atom) extends Link


object Mem {
  import org.json4s._
  import org.json4s.native.JsonMethods._
  import collection.mutable.ArrayBuffer

  def fromString(s: String): Mem = {
    val rootMem = buildMem(parse(s), null)
    val atomFromID = atomMapIn(rootMem)
    configureLinks(rootMem, atomFromID)
    rootMem
  }

  private def configureLinks(mem: Mem, atomFromID: Map[ID, Atom]) {
    val newAtoms = ArrayBuffer.empty[Atom]
    for (atom <- mem.atoms) {
      val links = atom.links
      for ((link, i) <- links.zipWithIndex) link match {
        case Raw(attr, data) => {
          if (attr.isRef) {
            links(i) = Ref(atomFromID(IntID(data.toInt)))
          } else {
            val node =
              if (attr.isHL) new Atom(HLAtomID(data.toInt), "!"+data, mem, Array(Ref(atom)))
              else           new Atom(DataAtomID(atom.id, i), data, mem, Array(Ref(atom)))
            links(i) = Ref(node)
            newAtoms += node
          }
        }
      }
    }
    for (g <- mem.mems) configureLinks(g, atomFromID)
    mem.atoms ++= newAtoms
  }

  private def atomMapIn(mem: Mem): Map[ID, Atom] = {
    val b = Map.newBuilder[ID, Atom]

    for (n <- mem.atoms) b += n.id -> n
    for (n <- mem.mems) b ++= atomMapIn(n)

    b.result
  }

  private def buildMem(json: JValue, parent: Mem): Mem = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"
    val mem = new Mem(IntID(id.toInt), name, parent)
    mem.atoms ++= atoms.map(buildAtom(_, mem))
    mem.mems ++= mems.map(buildMem(_, mem))
    mem
  }

  private def buildAtom(json: JValue, parent: Mem): Atom = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)
    new Atom(IntID(id.toInt), name, parent, links.map(buildLink _).toArray)
  }

  private def buildLink(json: JValue): Link = {
    val JInt(attr) = json \ "attr"
    val data = (json \ "data") match {
      case JString(s) => s
      case JDouble(d) => d.toString
      case JInt(i)    => i.toString
      case j          => throw new Exception("Unexpected data : " + j.toString)
    }
    Raw(Attribute(attr.toInt), data)
  }
}


import unyo.util._

class ViewContext {
  private val viewNodeFromID = collection.mutable.Map.empty[ID, View]
  val r = new util.Random

  def viewOf(node: Atom): View = viewNodeFromID.getOrElseUpdate(node.id, {
    val dim = node.id match {
      case HLAtomID(_) => Dim(10, 10)
      case _           => Dim(40, 40)
    }
    new View(Rect(Point(r.nextDouble * 800, r.nextDouble * 800), dim))
  })

  def viewOf(graph: Mem): View = viewNodeFromID.getOrElseUpdate(graph.id, {
    new View(coverableRect(graph))
  })

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
