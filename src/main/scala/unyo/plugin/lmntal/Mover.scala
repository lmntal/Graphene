package unyo.plugin.lmntal

import unyo.util._
import unyo.model._
import unyo.algorithm.{ForceBased}

object DefaultMover extends LMNtal.Mover {

  private def transaction(graph: Graph)(f: => Unit): Unit = {
    for (node <- graph.allNodes) node.view.reset
    f
    for (node <- graph.allNodes) node.view.move
  }

  private def coverableRect(node: Node): Rect = {
    if (node.childNodes.isEmpty) Rect(Point(Random.double * 800, Random.double * 800), Dim(80, 80))
    else                         node.childNodes.map(_.view.rect).reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
  }

  def moveAll(graph: Graph, elapsedSec: Double): Unit =
    moveAll(graph, elapsedSec, LMNtal.config.forces)

  def moveAll(graph: Graph, elapsedSec: Double, params: ForceParams): Unit = {
    if (graph == null) return
    transaction(graph) {
      move(graph.rootNode, elapsedSec, Point.zero, params)
    }
    resize(graph.rootNode)
  }

  private def move(node: Node, elapsedSec: Double, parentForce: Point, params: ForceParams): Unit = {
    if (node.view.fixed || node.view.selected) return

    val force = forceOfRepulsion(node, params) +
                forceOfSpring(node, params) +
                forceOfContraction(node, params) +
                parentForce

    node.view.affect(Point.zero, force, elapsedSec)

    val childNodes = if (unyo.core.Env.isMultiCoreEnabled) node.childNodes.par else node.childNodes
    for (n <- childNodes) move(n, elapsedSec, force / node.childNodes.size, params)
  }

  private def resize(node: Node): Unit = {
    for (n <- node.childNodes) resize(n)

    node.attr match {
      case Mem() if !node.childNodes.isEmpty => node.view.rect = coverableRect(node)
      case _ =>
    }
  }

  private def forceOfRepulsion(self: Node, params: ForceParams): Point = {
    if (self.isRoot) {
      Point.zero
    } else {
      val ps = params.repulsion
      val selfRect = self.view.rect
      val otherRects = self.parent.childNodes.map { _.view.rect }
      ForceBased.repulsion(selfRect, otherRects, ps.coef1, ps.coef2)
    }
  }

  private def forceOfSpring(self: Node, params: ForceParams): Point = {
    val ps = params.spring
    val selfPoint = self.view.rect.center
    val otherPoints = self.neighborNodes.map { _.view.rect.center }
    ForceBased.spring(selfPoint, otherPoints, ps.constant, ps.length)
  }

  private def forceOfContraction(self: Node, params: ForceParams): Point = {
    val f1 = if (self.isRoot) Point.zero else forceOfContraction(self.parent, self, params)
    val f2 = self.childNodes.foldLeft(Point.zero) { (res, other) => res + forceOfContraction(self, other, params) }
    f1 - f2
  }

  private def forceOfContraction(parent: Node, child: Node, params: ForceParams): Point = {
    val ps = params.contraction
    val surplusArea = parent.view.rect.area - parent.allChildNodes.size * ps.areaPerNode
    if (parent.isRoot || surplusArea < ps.threshold) {
      Point.zero
    } else {
      ForceBased.attraction(child.view.rect.center, parent.view.rect.center, ps.coef, math.sqrt(surplusArea))
    }
  }

}
