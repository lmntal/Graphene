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

  def moveAll(graph: Graph, elapsedSec: Double): Unit = {
    if (graph == null) return
    transaction(graph) {
      move(graph.rootNode, elapsedSec, Point.zero)
    }
    resize(graph.rootNode)
  }

  private def move(node: Node, elapsedSec: Double, parentForce: Point): Unit = {
    if (node.view.fixed || node.view.selected) return

    val force = forceOfRepulsion(node) +
                forceOfSpring(node) +
                forceOfContraction(node) +
                parentForce

    node.view.affect(Point.zero, force, elapsedSec)

    val childNodes = if (unyo.core.Env.isMultiCoreEnabled) node.childNodes.par else node.childNodes
    for (n <- childNodes) move(n, elapsedSec, force / node.childNodes.size)
  }

  private def resize(node: Node): Unit = {
    for (n <- node.childNodes) resize(n)

    node.attr match {
      case Mem() if !node.childNodes.isEmpty => node.view.rect = coverableRect(node)
      case _ =>
    }
  }

  private def forceOfRepulsion(self: Node): Point = {
    if (self.isRoot) {
      Point.zero
    } else {
      val params = LMNtal.config.forces.repulsion
      val selfRect = self.view.rect
      val otherRects = self.parent.childNodes.map { _.view.rect }
      ForceBased.repulsion(selfRect, otherRects, params.coef1, params.coef2)
    }
  }

  private def forceOfSpring(self: Node): Point = {
    val params = LMNtal.config.forces.spring
    val selfPoint = self.view.rect.center
    val otherPoints = self.neighborNodes.map { _.view.rect.center }
    ForceBased.spring(selfPoint, otherPoints, params.constant, params.length)
  }

  private def forceOfContraction(self: Node): Point = {
    val f1 = if (self.isRoot) Point.zero else forceOfContraction(self.parent, self)
    val f2 = self.childNodes.foldLeft(Point.zero) { (res, other) => res + forceOfContraction(self, other) }
    f1 - f2
  }

  private def forceOfContraction(parent: Node, child: Node): Point = {
    val params = LMNtal.config.forces.contraction
    val surplusArea = parent.view.rect.area - parent.allChildNodes.size * params.areaPerNode
    if (parent.isRoot || surplusArea < params.threshold) {
      Point.zero
    } else {
      ForceBased.attraction(child.view.rect.center, parent.view.rect.center, params.coef, math.sqrt(surplusArea))
    }
  }

}
