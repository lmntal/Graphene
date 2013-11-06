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
  def targetPos = value | 0x7F
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

object LMN {

  import org.json4s._
  import org.json4s.native.JsonMethods._
  import unyo.model.{Builder,Graph}
  import unyo.model.Builder.MutableNode

  def fromString(s: String): Graph = {
    val builder = new Builder
    buildMem(builder, parse(s), null)
    builder.build
  }

  case class IntID(value: Int) extends unyo.model.ID
  case class DataAtomID(id: unyo.model.ID, pos: Int) extends unyo.model.ID

  def buildMem(builder: Builder, json: JValue, parent: MutableNode) {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"

    val node = if (parent == null) builder.addRoot(IntID(id.toInt), name) else builder.addNode(IntID(id.toInt), name, parent)
    for (m <-  mems) buildMem( builder, m, node)
    for (a <- atoms) buildAtom(builder, a, node)
  }

  def buildAtom(builder: Builder, json: JValue, parent: MutableNode) {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)

    val node = builder.addNode(IntID(id.toInt), name, parent)
    for ((l,i) <- links.zipWithIndex) buildLink(builder, l, parent, node, i)
  }

  def buildLink(builder: Builder, json: JValue, parent: MutableNode, buddy: MutableNode, pos: Int) {
    val JInt(attr) = json \ "attr"
    val data = (json \ "data") match {
      case JString(s) => s
      case JDouble(d) => d.toString
      case JInt(i)    => i.toString
      case j          => throw new Exception("Unexpected data : " + j.toString)
    }
    val attribute = Attribute(attr.toInt)
    if (attribute.isRef) {
      (json \ "data") match {
        case JInt(i) => builder.addEdge(buddy.id, pos, IntID(i.toInt), attribute.targetPos)
        case j          => throw new Exception("Unexpected data : " + j.toString)
      }
    } else {
      val data = (json \ "data") match {
        case JString(s) => s
        case JDouble(d) => d.toString
        case JInt(i)    => i.toString
        case j          => throw new Exception("Unexpected data : " + j.toString)
      }
      val id = DataAtomID(buddy.id, pos)
      builder.addNode(id, data, parent)
      builder.addEdge(buddy.id, pos, id, 0)
    }
  }

}

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
