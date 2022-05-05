package graphene.plugin.lmntal

import java.io.{BufferedReader, InputStreamReader, PrintWriter, OutputStreamWriter}
import java.io.{File, IOException}

import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import graphene.model._
import graphene.util._

class LMNtalSource extends LMNtal.Source {

  case class Functor(name: String, arity: Int)

  val logger = Logger(LoggerFactory.getLogger("LMNtalSource"))

  private var runtime: Runtime = _
  private var graph: Graph = _
  private var colorGen: RandomColorGenerator = _
  private val colorFromFunctor = collection.mutable.Map.empty[Functor, java.awt.Color]

  private def coloring(graph: Graph): Graph = {
    for (n <- graph.allNodes) {
      n.view.color = n.attr match {
        case Atom => colorFromFunctor.getOrElseUpdate(Functor(n.name, n.arity), colorGen.next)
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

private class Runtime(commands: Seq[String]) extends collection.Iterator[String] {

  val logger = Logger(LoggerFactory.getLogger("Runtime"))

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


object Atom extends Attr

object HLAtom extends Attr

object Mem extends Attr


private object LMN {

  import collection.mutable

  import org.json4s._
  import org.json4s.native.JsonMethods._

  import graphene.model._


  def fromString(s: String): Graph = removeProxies(buildGraph(toJMem(parse(s))))

  case class JMem(id: Int, name: String, atoms: Seq[JAtom], mems: Seq[JMem])

  case class JAtom(id: Int, name: String, links: Seq[JLink])

  trait JLink

  case class JRef(id: Int, pos: Int) extends JLink

  case class JDataAtom(value: String) extends JLink

  case class JHLAtom(value: String) extends JLink

  private def toJMem(json: JValue): JMem = {
//    System.out.println("toJMem  : " + json.toString)
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    val JArray(atoms) = json \ "atoms"
    val JArray(mems) = json \ "membranes"
    JMem(id.toInt, name, atoms.map(toJAtom), mems.map(toJMem))
  }

  private def toJAtom(json: JValue): JAtom = {
//    System.out.println("toJAtom : " + json.toString)
    val JInt(id) = json \ "id"
    val JString(name) = json \ "name"
    var JArray(links) = json \ "links"
    if (name == "$in" || name == "$out") links = links.take(2)
    JAtom(id.toInt, name, links.map(toJLink))
  }

  private def toJLink(json: JValue): JLink = {
//    System.out.println("toJLink : " + json.toString)

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
        case JInt(i) => i.toString
        case j => throw new Exception("Unexpected data : " + j.toString)
      }
      if (isHL(attr)) JHLAtom(value) else JDataAtom(value)
    }
  }


  case class AtomID(value: Int) extends graphene.model.ID

  case class MemID(value: Int) extends graphene.model.ID

  case class DataAtomID(id: graphene.model.ID, pos: Int) extends graphene.model.ID

  case class HLAtomID(value: Int) extends graphene.model.ID

  val links = mutable.Set.empty[Set[(ID, Int)]]

  private def buildGraph(jmem: JMem): Graph = {
    val JMem(id, name, atoms, mems) = jmem

    val gctx = graphene.core.gui.MainFrame.instance.mainPanel.graphicsContext
    val graph = new Graph {
      viewBuilder = (n: Node) => {
        val rect = n.attr match {
          case Atom => Rect(Point.randomPointIn(gctx.wRect), Dim(24, 24))
          case HLAtom => Rect(Point.randomPointIn(gctx.wRect), Dim(12, 12))
          case _ => Rect(Point(0, 0), Dim(10, 10))
        }
        new View(rect, java.awt.Color.BLACK)
      }
    }

    val node = graph.createRootNode(MemID(id), name, Mem)
    links.clear()

    for (m <- mems) buildMem(m, node)
    for (a <- atoms) buildAtom(a, node)

    for (linkSet <- links) {
      val ls = linkSet.toSeq
      graph.createEdge(ls(0)._1, ls(1)._1)
    }

    graph
  }

  private def buildMem(jmem: JMem, parent: Node): Unit = {
    val JMem(id, name, atoms, mems) = jmem

    val node = parent.createNode(MemID(id.toInt), name, Mem)

    for (m <- mems) buildMem(m, node)
    for (a <- atoms) buildAtom(a, node)
  }

  private def buildAtom(jatom: JAtom, parent: Node): Unit = {
    val JAtom(id, name, links) = jatom

    val node = parent.createNode(AtomID(id.toInt), name, Atom)
    for ((l, i) <- links.zipWithIndex) buildLink(l, parent, node, i)
  }

  private def buildLink(jlink: JLink, parent: Node, buddy: Node, buddyPos: Int): Unit = {
    jlink match {
      case JRef(id, pos) => links += Set((buddy.id, buddyPos), (AtomID(id), pos))
      case JDataAtom(value) => {
        val id = DataAtomID(buddy.id, buddyPos)
        val node = parent.createNode(id, value, Atom)
        links += Set((id, 0), (buddy.id, buddyPos))
      }
      case JHLAtom(value) => {
        val id = HLAtomID(value.toInt)
        val node = parent.createNode(id, value, HLAtom)
        links += Set((id, 0), (buddy.id, buddyPos))
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
    val edge1 = proxy.edges(0)
    val edge2 = proxy.edges(1)
    val node1 = edge1.adjacentNodeOf(proxy)
    val node2 = edge2.adjacentNodeOf(proxy)

    proxy.graph.removeEdge(edge1)
    proxy.graph.removeEdge(edge2)

    proxy.graph.createEdge(node1, node2)
  }

}
