package unyo.plugin.lmntal

import unyo.utility._
import unyo.utility.model._

class DefaultMover extends LMNtalPlugin.Mover {

  var vctx: ViewContext = null
  def moveAll(vctx: ViewContext, elapsedSec: Double) {
    if (vctx == null || vctx.graph == null) return
    this.vctx = vctx
    vctx.transaction {
      move(vctx.graph.rootNode, elapsedSec, Point(0, 0))
    }
    resize(vctx.graph.rootNode)
  }

  private def move(node: Node, elapsedSec: Double, parentForce: Point) {
    val vec = forceOfRepulsion(node) +
              forceOfSpring(node) +
              forceOfContraction(node) +
              parentForce
    val view = vctx.viewOf(node)

    view.affect(Point(0, 0), vec, elapsedSec)

    for (n <- vctx.childNodesOf(node)) move(n, elapsedSec, vec / vctx.childNodesOf(node).size)
  }

  private def resize(node: Node) {
    for (n <- vctx.childNodesOf(node)) resize(n)

    if (!vctx.childNodesOf(node).isEmpty) {
      node.attribute match {
        case Mem() => vctx.viewOf(node).rect = vctx.coverableRect(node)
        case _     =>
      }
    }
  }

  private def forceOfRepulsion(self: Node): Point = {
    if (self.parent == null) {
      Point(0, 0)
    } else {
      vctx.childNodesOf(self.parent).filter { other =>
        self.id != other.id && self.parent.id == other.parent.id
      }.foldLeft(Point(0,0)) { (res, other) =>
        res + forceOfRepulsionBetween(self, other)
      }
    }
  }

  private def forceOfRepulsionBetween(lhs: Node, rhs: Node): Point = {
    val config = LMNtalPlugin.config
    val lrect = vctx.viewOf(lhs).rect
    val rrect = vctx.viewOf(rhs).rect
    val d = vctx.viewOf(lhs).rect.center - vctx.viewOf(rhs).rect.center
    val distance = lrect.distanceWith(rrect)
    val f = config.forces.repulsion.forceBetweenAtoms * (0.001 / (distance * distance / 1000 + 1))
    d.unit * f
  }

  private def forceOfSpring(self: Node): Point = {
    vctx.neighborNodesOf(self).foldLeft(Point(0,0)) { (res, other) => res + forceOfStringBetween(self, other) }
  }

  private def forceOfStringBetween(lhs: Node, rhs: Node): Point = {
    val config = LMNtalPlugin.config
    val d = vctx.viewOf(rhs).rect.center - vctx.viewOf(lhs).rect.center
    val f = config.forces.spring.force * (d.abs - config.forces.spring.length)
    d.unit * f
  }

  private def forceOfContraction(self: Node): Point = {
    // TODO: a bit dirty
    val view = vctx.viewOf(self)
    val f1 = if (self.parent == null) Point(0, 0) else forceOfContraction(self.parent, self)
    val f2 = vctx.childNodesOf(self).view.map(forceOfContraction(self, _)).foldLeft(Point(0, 0))(_ + _)
    f1 - f2
  }

  private def forceOfContraction(parent: Node, child: Node): Point = {
    val surplusArea = vctx.viewOf(parent).rect.area - vctx.allChildNodesOf(parent).size * 10000
    if (parent.isRoot || surplusArea < 100) {
      Point(0, 0)
    } else {
      val parentView = vctx.viewOf(parent)
      val childView = vctx.viewOf(child)
      val d = parentView.rect.center - childView.rect.center
      d.unit * math.sqrt(d.abs * math.sqrt(surplusArea)) / 2
    }
  }

}
