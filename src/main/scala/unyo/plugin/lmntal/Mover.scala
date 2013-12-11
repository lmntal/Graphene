package unyo.plugin.lmntal

import unyo.util._
import unyo.model._
import unyo.algorithm.{ForceBased}

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

    if (view.fixed) return

    view.affect(Point(0, 0), vec, elapsedSec)

    for (n <- node.childNodes) move(n, elapsedSec, vec / node.childNodes.size)
  }

  private def resize(node: Node) {
    for (n <- node.childNodes) resize(n)

    if (!node.childNodes.isEmpty) {
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
      val params = LMNtalPlugin.config.forces.repulsion
      val selfView = vctx.viewOf(self).rect
      val otherViews = self.parent.childNodes.map(vctx.viewOf(_).rect)
      ForceBased.repulsion(selfView, otherViews, params.coef1, params.coef2)
    }
  }

  private def forceOfSpring(self: Node): Point = {
    val params = LMNtalPlugin.config.forces.spring
    val selfPoint = vctx.viewOf(self).rect.center
    val otherPoints = self.neighborNodes.map(vctx.viewOf(_).rect.center)
    ForceBased.spring(selfPoint, otherPoints, params.constant, params.length)
  }

  private def forceOfContraction(self: Node): Point = {
    // TODO: a bit dirty
    val view = vctx.viewOf(self)
    val f1 = if (self.parent == null) Point(0, 0) else forceOfContraction(self.parent, self)
    val f2 = self.childNodes.view.map(forceOfContraction(self, _)).foldLeft(Point(0, 0))(_ + _)
    f1 - f2
  }

  private def forceOfContraction(parent: Node, child: Node): Point = {
    val params = LMNtalPlugin.config.forces.contraction
    val surplusArea = vctx.viewOf(parent).rect.area - parent.allChildNodes.size * params.areaPerNode
    if (parent.isRoot || surplusArea < params.threshold) {
      Point(0, 0)
    } else {
      val parentView = vctx.viewOf(parent)
      val childView = vctx.viewOf(child)
      ForceBased.attraction(childView.rect.center, parentView.rect.center, params.coef, math.sqrt(surplusArea))
    }
  }

}
