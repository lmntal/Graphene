package unyo.plugin.lmntal

import unyo.util._

class DefaultMover extends LMNtalPlugin.Mover {

  var viewContext: ViewContext = null
  def moveAll(viewContext: ViewContext, elapsedSec: Double) {
    if (viewContext == null || viewContext.rootMem == null) return
    this.viewContext = viewContext
    move(viewContext.rootMem, elapsedSec)
  }

  def move(graph: Mem, elapsedSec: Double) {
    for (subgraph <- graph.mems) move(subgraph, elapsedSec)

    for (node <- graph.atoms) {
      val v1 = viewContext.viewOf(node)
      var vec = Point(0, 0)

      vec = vec + forceOfReplusion(node)
      vec = vec + forceOfSpring(node)

      v1.force(vec, elapsedSec)
    }

    resizeGraphArea(graph)
  }

  private def resizeGraphArea(graph: Mem) {
    graph.mems.foreach(resizeGraphArea(_))
    val view = viewContext.viewOf(graph)
    view.rect = viewContext.coverableRect(graph)
  }

  private def forceOfReplusion(self: Atom): Point = {
    var vec = Point(0, 0)
    val v1 = viewContext.viewOf(self)
    for (other <- self.parent.atoms) {
      if (self.id != other.id && self.parent.id == other.parent.id) {
        val v2 = viewContext.viewOf(other)
        val d = v2.rect.center - v1.rect.center
        val f = 1000000.0 / d.sqabs
        vec = vec - d.unit * f
      }
    }
    for (other <- self.parent.mems) {
      if (self.id != other.id && self.parent.id == other.parent.id) {
        val v2 = viewContext.viewOf(other)
        if (v1.rect.isCrossingWith(v2.rect)) {
          val d = v2.rect.center - v1.rect.center
          val f = 10000
          vec = vec - d.unit * f
        }
      }
    }
    vec
  }

  private def forceOfSpring(self: Atom): Point = {
    var vec = Point(0, 0)
    val v1 = viewContext.viewOf(self)
    for (i <- 0 until self.arity) {
      val other = self.buddyAt(i)
      val v2 = viewContext.viewOf(other)
      val d = v2.rect.center - v1.rect.center
      val f = 2.0 * (d.abs - 120)
      vec = vec + d.unit * f
    }
    vec
  }

}
