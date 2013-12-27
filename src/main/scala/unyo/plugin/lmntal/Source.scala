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

  def coloring(graph: Graph): Graph = {
    for (n <- graph.allNodes) {
      n.view.color = n.attr match {
        case Atom() => colorFromFunctor.getOrElseUpdate(Functor(n.name, n.arity), colorGen.next)
        case _ => Palette.lightGray
      }
    }
    graph
  }

  def run(options: Seq[String]): Graph = {
    runtime = new Runtime(Buffer("env", s"LMNTAL_HOME=${LMNtal.config.lmntalHome}", LMNtal.config.slimPath, "-t", "--dump-json", "--hl") ++ options)

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

  val reader = {
    val pb = new ProcessBuilder(commands)
    logger.info("run process: " + pb.command.mkString(" "))
    pb.redirectErrorStream(true)
    val p = pb.start
    new BufferedReader(new InputStreamReader(p.getInputStream))
  }

  val iter = Iterator.continually(reader.readLine).takeWhile(_ != null)

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


  def fromString(s: String): Graph = removeProxies(buildGraph(parse(s)))

  case class IntID(value: Int) extends unyo.model.ID
  case class DataAtomID(id: unyo.model.ID, pos: Int) extends unyo.model.ID
  case class HLAtomID(value: Int) extends unyo.model.ID

  private def buildGraph(json: JValue): Graph = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"

    val node = new Node(IntID(id.toInt), name, Mem())
    val graph = new Graph(node)

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

  private def buildMem(json: JValue, parent: Node): Unit = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"

    val node = new Node(IntID(id.toInt), name, Mem())
    parent.addChildNode(node)
    for (m <-  mems) buildMem (m, node)
    for (a <- atoms) buildAtom(a, node)
  }

  private def buildAtom(json: JValue, parent: Node): Unit = {
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)

    val node = Node(IntID(id.toInt), name, Atom())
    parent.addChildNode(node)
    for ((l,i) <- links.zipWithIndex) buildLink(l, parent, node, i)
  }

  private def buildLink(json: JValue, parent: Node, buddy: Node, buddyPos: Int): Unit = {
    case class Attribute(value: Int) {
      def isRef = (value & 0x80) == 0
      def targetPos = value & 0x7F
      def isData = !isRef
      def isHL = value == 0x8a
    }

    val JInt(attr) = json \ "attr"
    val attribute = Attribute(attr.toInt)

    if (attribute.isRef) {
      (json \ "data") match {
        case JInt(i) => buddy.addEdgeTo(Port(IntID(i.toInt), attribute.targetPos))
        case j       => throw new Exception("Unexpected data : " + j.toString)
      }
    } else {
      val data = (json \ "data") match {
        case JString(s) => s
        case JDouble(d) => d.toString
        case JInt(i)    => i.toString
        case j          => throw new Exception("Unexpected data : " + j.toString)
      }
      val id = if (attribute.isHL) HLAtomID(data.toInt) else DataAtomID(buddy.id, buddyPos)
      val attr = if (attribute.isHL) HLAtom() else Atom()
      val node = new Node(id, data, attr)
      parent.addChildNode(node)
      buddy.addEdgeTo(Port(id, 0))
      node.addEdgeTo(Port(buddy.id, buddyPos))
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
