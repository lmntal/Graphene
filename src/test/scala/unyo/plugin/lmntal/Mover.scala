package unyo.plugin.lmntal

import org.specs2.mutable._
import org.specs2.matcher.Matcher
import org.specs2.specification.Scope

import unyo.util._

class FastMoverSpec extends Specification {

  def beAlmostSame(d1: Double): Matcher[Double] = beLike { case d2 =>
    if (d1 != 0.0 && d2 != 0.0) {
      (d1 / d2) must beBetween(0.999999999, 1.000000001)
    } else {
      d1 === d2
    }
  }

  def beAlmostSame(p: Point): Matcher[Point] = beLike { case Point(x, y) =>
    (p.x must beAlmostSame(x)) and (p.y must beAlmostSame(y))
  }

  "FastMover.forceFor" should {
    "behave in the same way as DefaultMover.forceFor" in new Graphs {
      val params = new ForceParams
      params.contraction.coef = 100
      for (n <- graph1.allNodes) {
        val f1 = DefaultMover.forceFor(n, params)
        val f2 = FastMover.forceFor(n, params)
        f1 must beAlmostSame(f2)
      }
    }
  }

  "FastMover.forceOfContraction" should {
    "behave in the same way as DefaultMover.forceOfContraction" in new Graphs {
      val params = new ForceParams
      params.contraction.coef = 100
      for (n <- graph1.allNodes) {
        val f1 = DefaultMover.forceOfContraction(n, params)
        val f2 = FastMover.forceOfContraction(n, params)
        f1 must beAlmostSame(f2)
      }
    }
  }

}

trait Graphs extends Scope {
  import unyo.model._

  case class IntID(value: Int) extends ID

  /**
   * LMNtal code:
   * { a(X), b(Y) }, { c(X, Z), { d(Y, Z, W) } }, e(W).
   */
  val graph1 = {
    val root = Node(IntID(0), "")
    val g = new Graph(root)
    val r = Rect(Point(0, 0), Dim(40000, 30000))
    g.viewBuilder = (n: Node) => {
      new View(Rect(Point.randomPointIn(r), Dim(12, 12)), java.awt.Color.WHITE)
    }

    val a = Node(IntID(1), "a")
    val b = Node(IntID(2), "b")
    val c = Node(IntID(3), "c")
    val d = Node(IntID(4), "d")
    val e = Node(IntID(5), "e")

    val mem1 = Node(IntID(6), "")
    root.addChildNode(mem1)
    mem1.addChildNode(a)
    mem1.addChildNode(b)

    val mem2 = Node(IntID(7), "")
    root.addChildNode(mem2)
    mem2.addChildNode(c)
    mem2.addChildNode(d)

    root.addChildNode(e)

    a.addEdgeTo(Port(IntID(3), 0))
    b.addEdgeTo(Port(IntID(4), 0))
    c.addEdgeTo(Port(IntID(1), 0))
    c.addEdgeTo(Port(IntID(4), 1))
    d.addEdgeTo(Port(IntID(2), 0))
    d.addEdgeTo(Port(IntID(3), 1))
    d.addEdgeTo(Port(IntID(5), 0))
    e.addEdgeTo(Port(IntID(4), 2))

    def coverableRect(node: Node): Rect = {
      if (node.childNodes.isEmpty) Rect(Point(Random.double * 800, Random.double * 800), Dim(80, 80))
        else                         node.childNodes.map(_.view.rect).reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
    }

    def resize(node: Node): Unit = {
      for (n <- node.childNodes) resize(n)

      if (!node.childNodes.isEmpty) node.view.rect = coverableRect(node)
    }

    resize(g.rootNode)

    g
  }

}
