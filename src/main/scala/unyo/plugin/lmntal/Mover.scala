package unyo.plugin.lmntal

import unyo.utility._
import unyo.utility.model._

class DefaultMover extends LMNtalPlugin.Mover {

  var vctx: ViewContext = null
  def moveAll(vctx: ViewContext, elapsedSec: Double) {
    if (vctx == null || vctx.graph == null) return
    this.vctx = vctx
    move(vctx.graph.rootNode, elapsedSec)
  }

  def move(node: Node, elapsedSec: Double) {
    val vec = forceOfRepulsion(node) +
              forceOfSpring(node)

    vctx.viewOf(node).force(vec, elapsedSec)

    for (n <- node.childNodes) move(n, elapsedSec)

    resizeGraphArea(node)
  }

  private def resizeGraphArea(node: Node) {
    if (!node.childNodes.isEmpty) {
      node.attribute match {
        case Mem() => {
          for (n <- node.childNodes) resizeGraphArea(n)
          vctx.viewOf(node).rect = vctx.coverableRect(node)
        }
        case _     =>
      }
    }
  }

  private def forceOfRepulsion(self: Node): Point = {
    if (self.parent == null) {
      Point(0, 0)
    } else {
      self.parent.childNodes.view.filter { other =>
        self.id != other.id && self.parent.id == other.parent.id
      }.foldLeft(Point(0,0)) { (res, other) =>
        res + forceOfRepulsionBetween(self, other)
      }
    }
  }

  private def forceOfRepulsionBetween(lhs: Node, rhs: Node): Point = {
    val config = LMNtalPlugin.config
    val d = vctx.viewOf(lhs).rect.center - vctx.viewOf(rhs).rect.center
    val f = config.forces.repulsion.forceBetweenAtoms / d.sqabs
    d.unit * f
  }

  private def forceOfSpring(self: Node): Point = {
    self.neighborNodes.foldLeft(Point(0,0)) { (res, other) => res + forceOfStringBetween(self, other) }
  }

  private def forceOfStringBetween(lhs: Node, rhs: Node): Point = {
    val config = LMNtalPlugin.config
    val d = vctx.viewOf(rhs).rect.center - vctx.viewOf(lhs).rect.center
    val f = config.forces.spring.force * (d.abs - config.forces.spring.length)
    d.unit * f
  }

}
