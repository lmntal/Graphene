package unyo.plugin.lmntal

import collection.mutable.Set

case class Attribute(value: Int) {
  def isRef = (value & 0x80) == 0
  def targetPos = value & 0x7F
  def isData = !isRef
  def isHL = value == 0x8a
}

case class Atom() extends unyo.model.Attr
case class HLAtom() extends unyo.model.Attr
case class Mem() extends unyo.model.Attr

object LMN {

  import org.json4s._
  import org.json4s.native.JsonMethods._
  import unyo.model.{Builder,Graph}
  import unyo.model.{Builder,NodeBuilder,EdgeBuilder}

  def fromString(s: String): Graph = {
    val builder = new Builder
    buildMem(builder, parse(s), null)
    removeProxies(builder)
    builder.build
  }

  case class IntID(value: Int) extends unyo.model.ID
  case class DataAtomID(id: unyo.model.ID, pos: Int) extends unyo.model.ID
  case class HLAtomID(value: Int) extends unyo.model.ID

  def buildMem(builder: Builder, json: JValue, parent: NodeBuilder) { val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"

    val node = if (parent == null) builder.addRoot(IntID(id.toInt), name) else parent.addChildNode(IntID(id.toInt), name)
    node.attribute = Mem()
    for (m <-  mems) buildMem( builder, m, node)
    for (a <- atoms) buildAtom(builder, a, node)
  }

  def buildAtom(builder: Builder, json: JValue, parent: NodeBuilder) {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)

    val node = parent.addChildNode(IntID(id.toInt), name)
    node.attribute = Atom()
    for ((l,i) <- links.zipWithIndex) buildLink(builder, l, parent, node, i)
  }

  def buildLink(builder: Builder, json: JValue, parent: NodeBuilder, buddy: NodeBuilder, pos: Int) {
    val JInt(attr) = json \ "attr"
    val attribute = Attribute(attr.toInt)
    if (attribute.isRef) {
      (json \ "data") match {
        case JInt(i) => buddy.addEdge(pos, IntID(i.toInt), attribute.targetPos)
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
      val node = parent.addChildNode(id, data)
      node.attribute = if (attribute.isHL) HLAtom() else Atom()
      buddy.addEdge(pos, id, 0)
      node.addEdge(0, buddy.id, pos)
    }
  }

  def removeProxies(builder: Builder) {
    val proxies = removeProxies(builder, builder.root)
    for (n <- proxies) builder.removeNode(n.id)
  }
  private def isProxy(node: NodeBuilder) = node.name == "$in" || node.name == "$out"

  private def searchActualBuddy(self: NodeBuilder): (NodeBuilder, Seq[NodeBuilder]) = {
    if (isProxy(self)) {
      val proxy = self.edges(0).targetNode
      val buddy = proxy.edges(1).targetNode
      val (actualBuddy, proxies) = searchActualBuddy(buddy)
      (actualBuddy, self +: proxy +: proxies)
    } else {
      (self, Seq.empty[NodeBuilder])
    }
  }

  def removeProxies(builder: Builder, node: NodeBuilder): Seq[NodeBuilder] = {
    if (isProxy(node)) return collection.mutable.ArrayBuffer.empty[NodeBuilder]

    var allProxies = Seq.empty[NodeBuilder]

    for (n <- node.childNodes) allProxies ++= removeProxies(builder, n)

    for (e1 <- node.edges if isProxy(e1.targetNode)) {
      val (other, proxies) = searchActualBuddy(e1.targetNode)
      val e2 = proxies.last.edges(1).reverseEdge

      e1.targetID = other.id
      e1.targetPos = e2.reverseEdge.targetPos

      e2.targetID = node.id
      e2.targetPos = e1.reverseEdge.targetPos

      allProxies ++= proxies
    }

    allProxies
  }

}
