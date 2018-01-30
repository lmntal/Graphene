package graphene.plugin.lmntal

import org.specs2.mutable._
import org.specs2.matcher.Matcher
import org.specs2.specification.Scope

import graphene.util._

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
  import graphene.model._

  case class IntID(value: Int) extends ID

  /**
   * LMNtal code:
   * { a(X), b(Y) }, { c(X, Z), { d(Y, Z, W) } }, e(W).
   */
  val graph1 = {
    val g = new Graph {
      val r = Rect(Point(0, 0), Dim(40000, 30000))
      viewBuilder = (n: Node) => {
        new View(Rect(Point.randomPointIn(r), Dim(12, 12)), java.awt.Color.WHITE)
      }
    }
    val root = g.createRootNode(IntID(0), "")

    val mem1 = root.createNode(IntID(6), "")
    val mem2 = root.createNode(IntID(7), "")

    val a = mem1.createNode(IntID(1), "a")
    val b = mem1.createNode(IntID(2), "b")
    val c = mem2.createNode(IntID(3), "c")
    val d = mem2.createNode(IntID(4), "d")
    val e = root.createNode(IntID(5), "e")

    g.createEdge(IntID(1), IntID(3))
    g.createEdge(IntID(2), IntID(4))
    g.createEdge(IntID(3), IntID(4))
    g.createEdge(IntID(4), IntID(5))

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
