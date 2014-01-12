package unyo.plugin.lmntal

import java.io.{BufferedReader,InputStreamReader,PrintWriter,OutputStreamWriter}
import java.io.{File,IOException}

import scala.actors.Actor._
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._

import com.typesafe.scalalogging.slf4j._

import unyo.model._
import unyo.util._

class LMNtalSource extends LMNtal.Source {

  case class Functor(name: String, arity: Int)

  private var runtime: Runtime = _
  private var graph: Graph = _
  private var colorGen: RandomColorGenerator = _
  private val colorFromFunctor = collection.mutable.Map.empty[Functor,java.awt.Color]

  private def coloring(graph: Graph): Graph = {
    for (n <- graph.allNodes) {
      n.view.color = n.attr match {
        case Atom() => colorFromFunctor.getOrElseUpdate(Functor(n.name, n.arity), colorGen.next)
        case _ => Palette.lightGray
      }
    }
    graph
  }

  def run(options: Seq[String]): Graph = {
    val additionalOptions = LMNtal.config.additionalOptions.split(' ').filter { o => !o.isEmpty }
    runtime = new Runtime(Buffer("env", s"LMNTAL_HOME=${LMNtal.config.lmntalHome}", LMNtal.config.slimPath, "-t", "--dump-json", "--hl") ++ additionalOptions ++ options)

    colorFromFunctor.clear
    colorGen = new RandomColorGenerator

    graph = coloring(LMN.fromString(runtime.next))
    graph
  }
  def current = graph
  def next = {
    graph = coloring(LMN.fromString(runtime.next).inheritViews(graph))
    graph
  }
  def hasNext = runtime.hasNext

}

private class Runtime(commands: Seq[String]) extends collection.Iterator[String] with Logging {

  private val reader = {
    val pb = new ProcessBuilder(commands)
    logger.info("run process: " + pb.command.mkString(" "))
    pb.redirectErrorStream(true)
    val p = pb.start
    new BufferedReader(new InputStreamReader(p.getInputStream))
  }

  private val iter = Iterator.continually(reader.readLine).takeWhile(_ != null)

  def hasNext = iter.hasNext
  def next = iter.next
}


case class Atom() extends unyo.model.Attr
case class HLAtom() extends unyo.model.Attr
case class Mem() extends unyo.model.Attr


private object LMN {

  import collection.mutable.Set

  import org.json4s._
  import org.json4s.native.JsonMethods._

  import unyo.model._


  def fromString(s: String): Graph = removeProxies(buildGraph(toJMem(parse(s))))

  case class JMem(id: Int, name: String, atoms: Seq[JAtom], mems: Seq[JMem])
  case class JAtom(id: Int, name: String, links: Seq[JLink]) {
    def isProxy = name == "$in" || name == "$out"
  }
  trait JLink
  case class JRef(id: Int, pos: Int) extends JLink
  case class JDataAtom(value: String) extends JLink
  case class JHLAtom(value: String) extends JLink

  private def toJMem(json: JValue): JMem = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"
    JMem(id.toInt, name, atoms.map(toJAtom), mems.map(toJMem))
  }

  private def toJAtom(json: JValue): JAtom = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    JAtom(id.toInt, name, links.map(toJLink))
  }

  private def toJLink(json: JValue): JLink = {
    def isRef(attr: Int) = (attr & 0x80) == 0
    def getPos(attr: Int) = attr & 0x7F
    def isHL(attr: Int) = attr == 0x8a

    val JInt(battr) = json \ "attr"
    val attr = battr.toInt
    if (isRef(attr)) {
      val JInt(i) = json \ "data"
      JRef(i.toInt, getPos(attr))
    } else {
      val value = (json \ "data") match {
        case JString(s) => s
        case JDouble(d) => d.toString
        case JInt(i)    => i.toString
        case j          => throw new Exception("Unexpected data : " + j.toString)
      }
      if (isHL(attr)) JHLAtom(value) else JDataAtom(value)
    }
  }


  case class AtomID(value: Int) extends unyo.model.ID
  case class MemID(value: Int) extends unyo.model.ID
  case class DataAtomID(id: unyo.model.ID, pos: Int) extends unyo.model.ID
  case class HLAtomID(value: Int) extends unyo.model.ID

  private def buildGraph(jmem: JMem): Graph = {
    val JMem(id, name, atoms, mems) = jmem

    val graph = new Graph
    val node = graph.createRootNode(MemID(id), name, Mem())

    val gctx = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext
    graph.viewBuilder = (n: Node) => {
      val rect = n.attr match {
        case Atom()   => Rect(Point.randomPointIn(gctx.wRect), Dim(24, 24))
        case HLAtom() => Rect(Point.randomPointIn(gctx.wRect), Dim(12, 12))
        case _        => Rect(Point(0, 0), Dim(10, 10))
      }
      new View(rect, java.awt.Color.BLACK)
    }

    for (m <-  mems) buildMem (m, node)
    for (a <- atoms) buildAtom(a, node)

    graph
  }

  private def buildMem(jmem: JMem, parent: Node): Unit = {
    val JMem(id, name, atoms, mems) = jmem

    val node = parent.createNode(MemID(id.toInt), name, Mem())

    for (m <-  mems) buildMem (m, node)
    for (a <- atoms) buildAtom(a, node)
  }

  private def buildAtom(jatom: JAtom, parent: Node): Unit = {
    val JAtom(id, name, links) = jatom

    val node = parent.createNode(AtomID(id.toInt), name, Atom())
    for ((l,i) <- (if (jatom.isProxy) links.take(2) else links).zipWithIndex) buildLink(l, parent, node, i)
  }

  private def buildLink(jlink: JLink, parent: Node, buddy: Node, buddyPos: Int): Unit = {
    jlink match {
      case JRef(id, pos) => {
        buddy.addEdgeTo(Port(AtomID(id), pos))
      }
      case JDataAtom(value) => {
        val id = DataAtomID(buddy.id, buddyPos)
        val node = parent.createNode(id, value, Atom())
        buddy.addEdgeTo(Port(id, 0))
        node.addEdgeTo(Port(buddy.id, buddyPos))
      }
      case JHLAtom(value) => {
        val id = HLAtomID(value.toInt)
        val node = parent.createNode(id, value, HLAtom())
        buddy.addEdgeTo(Port(id, 0))
        node.addEdgeTo(Port(buddy.id, buddyPos))
      }
    }
  }

  private def isProxy(node: Node) = node.name == "$in" || node.name == "$out"

  private def removeProxies(graph: Graph): Graph = {
    val proxies = graph.allNodes.filter(isProxy(_))
    for (p <- proxies) removeProxy(p)
    for (p <- proxies) p.removeFromParent
    graph
  }

  private def removeProxy(proxy: Node): Unit = {
    val port1 = proxy.edges(0).target
    val port2 = proxy.edges(1).target
    val node1 = proxy.neighborNodeAt(0)
    val node2 = proxy.neighborNodeAt(1)

    node1.edges(port1.pos).target = port2
    node2.edges(port2.pos).target = port1
  }

}
