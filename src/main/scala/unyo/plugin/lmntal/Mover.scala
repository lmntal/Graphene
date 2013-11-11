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
    for (n <- node.childNodes) move(n, elapsedSec)

    for (n <- node.childNodes) {
      val v1 = vctx.viewOf(n)
      var vec = Point(0, 0)

      vec = vec + forceOfRepulsion(n)
      vec = vec + forceOfSpring(n)

      v1.force(vec, elapsedSec)
    }

    // resizeGraphArea(node)
  }

  private def resizeGraphArea(node: Node) {
    node.childNodes.foreach(resizeGraphArea(_))
    val view = vctx.viewOf(node)
    view.rect = vctx.coverableRect(node)
  }

  private def forceOfRepulsion(self: Node): Point = {
    val config = LMNtalPlugin.config

    var vec = Point(0, 0)
    val v1 = vctx.viewOf(self)
    for (other <- self.parent.childNodes) {
      if (self.id != other.id && self.parent.id == other.parent.id) {
        val v2 = vctx.viewOf(other)
        val d = v2.rect.center - v1.rect.center
        val f = config.forces.repulsion.forceBetweenAtoms / d.sqabs
        vec = vec - d.unit * f
      }
    }
    for (other <- self.parent.childNodes) {
      if (self.id != other.id && self.parent.id == other.parent.id) {
        val v2 = vctx.viewOf(other)
        if (v1.rect.isCrossingWith(v2.rect)) {
          val d = v2.rect.center - v1.rect.center
          val f = config.forces.repulsion.forceBetweenMems
          vec = vec - d.unit * f
        }
      }
    }
    vec
  }

  private def forceOfSpring(self: Node): Point = {
    val config = LMNtalPlugin.config

    var vec = Point(0, 0)
    val v1 = vctx.viewOf(self)
    for (i <- 0 until self.neighborNodes.size) {
      val other = self.neighborNodes(i)
      val v2 = vctx.viewOf(other)
      val d = v2.rect.center - v1.rect.center
      val f = config.forces.spring.force * (d.abs - config.forces.spring.length)
      vec = vec + d.unit * f
    }
    vec
  }

}
