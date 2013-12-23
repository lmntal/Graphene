package unyo.plugin.lmntal

import unyo.util._
import unyo.model._
import unyo.algorithm.{ForceBased}

object DefaultMover {
  private def transaction(graph: Graph)(f: => Unit) {
    for (node <- graph.allNodes) node.view.reset
    f
    for (node <- graph.allNodes) node.view.move
  }
}

class DefaultMover extends LMNtal.Mover {

  private def coverableRect(node: Node): Rect = {
    val rects = node.childNodes.map(_.view.rect)
    if (rects.isEmpty) Rect(Point(Random.double * 800, Random.double * 800), Dim(80, 80))
    else               rects.reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
  }

  var graph: Graph = null
  def moveAll(graph: Graph, elapsedSec: Double) {
    if (graph == null) return
    this.graph = graph
    DefaultMover.transaction(graph) {
      move(graph.rootNode, elapsedSec, Point.zero)
    }
    resize(graph.rootNode)
  }

  private def move(node: Node, elapsedSec: Double, parentForce: Point) {
    val vec = forceOfRepulsion(node) +
              forceOfSpring(node) +
              forceOfContraction(node) +
              parentForce
    val view = node.view

    if (view.fixed) return

    view.affect(Point.zero, vec, elapsedSec)

    val childNodes = if (unyo.core.Env.isMultiCoreEnabled) node.childNodes.par else node.childNodes
    for (n <- childNodes) move(n, elapsedSec, vec / node.childNodes.size)
  }

  private def resize(node: Node) {
    for (n <- node.childNodes) resize(n)

    if (!node.childNodes.isEmpty) {
      node.attr match {
        case Mem() => node.view.rect = coverableRect(node)
        case _     =>
      }
    }
  }

  private def forceOfRepulsion(self: Node): Point = {
    if (self.parent == null) {
      Point.zero
    } else {
      val params = LMNtal.config.forces.repulsion
      val selfView = self.view.rect
      val otherViews = self.parent.childNodes.map(_.view.rect)
      ForceBased.repulsion(selfView, otherViews, params.coef1, params.coef2)
    }
  }

  private def forceOfSpring(self: Node): Point = {
    val params = LMNtal.config.forces.spring
    val selfPoint = self.view.rect.center
    val otherPoints = self.neighborNodes.map(_.view.rect.center)
    ForceBased.spring(selfPoint, otherPoints, params.constant, params.length)
  }

  private def forceOfContraction(self: Node): Point = {
    // TODO: a bit dirty
    val view = self.view
    val f1 = if (self.parent == null) Point.zero else forceOfContraction(self.parent, self)
    val f2 = self.childNodes.view.map(forceOfContraction(self, _)).foldLeft(Point.zero)(_ + _)
    f1 - f2
  }

  private def forceOfContraction(parent: Node, child: Node): Point = {
    val params = LMNtal.config.forces.contraction
    val surplusArea = parent.view.rect.area - parent.allChildNodes.size * params.areaPerNode
    if (parent.isRoot || surplusArea < params.threshold) {
      Point.zero
    } else {
      val parentView = parent.view
      val childView = child.view
      ForceBased.attraction(childView.rect.center, parentView.rect.center, params.coef, math.sqrt(surplusArea))
    }
  }

}
