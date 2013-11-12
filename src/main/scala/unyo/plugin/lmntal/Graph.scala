package unyo.plugin.lmntal

import collection.mutable.Set

case class Attribute(value: Int) {
  def isRef = (value & 0x80) == 0
  def targetPos = value | 0x7F
  def isData = !isRef
  def isHL = value == 0x8a
}

case class Atom() extends unyo.utility.model.Attr
case class HLAtom() extends unyo.utility.model.Attr
case class Mem() extends unyo.utility.model.Attr

object LMN {

  import org.json4s._
  import org.json4s.native.JsonMethods._
  import unyo.utility.model.{Builder,Graph}
  import unyo.utility.model.Builder.MutableNode

  def fromString(s: String): Graph = {
    val builder = new Builder
    buildMem(builder, parse(s), null)
    builder.build
  }

  case class IntID(value: Int) extends unyo.utility.model.ID
  case class DataAtomID(id: unyo.utility.model.ID, pos: Int) extends unyo.utility.model.ID
  case class HLAtomID(value: Int) extends unyo.utility.model.ID

  def buildMem(builder: Builder, json: JValue, parent: MutableNode) {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"

    val node = if (parent == null) builder.addRoot(IntID(id.toInt), name) else builder.addNode(IntID(id.toInt), name, parent)
    node.attribute = Mem()
    for (m <-  mems) buildMem( builder, m, node)
    for (a <- atoms) buildAtom(builder, a, node)
  }

  def buildAtom(builder: Builder, json: JValue, parent: MutableNode) {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)

    val node = builder.addNode(IntID(id.toInt), name, parent)
    node.attribute = Atom()
    for ((l,i) <- links.zipWithIndex) buildLink(builder, l, parent, node, i)
  }

  def buildLink(builder: Builder, json: JValue, parent: MutableNode, buddy: MutableNode, pos: Int) {
    val JInt(attr) = json \ "attr"
    val attribute = Attribute(attr.toInt)
    if (attribute.isRef) {
      (json \ "data") match {
        case JInt(i) => builder.addEdge(buddy.id, pos, IntID(i.toInt), attribute.targetPos)
        case j       => throw new Exception("Unexpected data : " + j.toString)
      }
    } else {
      val data = (json \ "data") match {
        case JString(s) => s
        case JDouble(d) => d.toString
        case JInt(i)    => i.toString
        case j          => throw new Exception("Unexpected data : " + j.toString)
      }
      val id = if (attribute.isHL) HLAtomID(data.toInt) else DataAtomID(buddy.id, pos)
      val node = builder.addNode(id, data, parent)
      node.attribute = if (attribute.isHL) HLAtom() else Atom()
      builder.addEdge(buddy.id, pos, id, 0)
      builder.addEdge(id, 0, buddy.id, pos)
    }
  }

}
