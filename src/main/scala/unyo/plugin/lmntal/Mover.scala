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

    val force = forceFor(node, params) + parentForce

    node.view.affect(force, elapsedSec)

    val childNodes = if (unyo.core.Env.isMultiCoreEnabled) node.childNodes.par else node.childNodes
    for (n <- childNodes) move(n, elapsedSec, force / node.childNodes.size, params)
  }

  private def resize(node: Node): Unit = {
    for (n <- node.childNodes) resize(n)

    node.attr match {
      case Mem if !node.childNodes.isEmpty => node.view.rect = coverableRect(node)
      case _ =>
    }
  }

  def forceFor(node: Node, params: ForceParams): Point =
    forceOfRepulsion(node, params) +
    forceOfSpring(node, params) +
    forceOfContraction(node, params)


  def forceOfRepulsion(self: Node, params: ForceParams): Point = {
    if (self.isRoot) {
      Point.zero
    } else {
      val ps = params.repulsion
      val selfRect = self.view.rect
      val otherRects = self.parent.childNodes.map { _.view.rect }
      ForceBased.repulsion(selfRect, otherRects, ps.coef1, ps.coef2)
    }
  }

  def forceOfSpring(self: Node, params: ForceParams): Point = {
    val ps = params.spring
    val selfPoint = self.view.rect.center
    val otherPoints = self.neighborNodes.map { _.view.rect.center }
    ForceBased.spring(selfPoint, otherPoints, ps.constant, ps.length)
  }

  def forceOfContraction(self: Node, params: ForceParams): Point = {
    val f1 = if (self.isRoot) Point.zero else forceOfContraction(self.parent, self, params)
    val f2 = self.childNodes.foldLeft(Point.zero) { (res, other) => res + forceOfContraction(self, other, params) }
    f1 - f2
  }

  def forceOfContraction(parent: Node, child: Node, params: ForceParams): Point = {
    val ps = params.contraction
    val surplusArea = parent.view.rect.area - parent.allChildNodes.size * ps.areaPerNode - ps.threshold
    if (parent.isRoot || surplusArea <= 0) {
      Point.zero
    } else {
      ForceBased.attraction(child.view.rect.center, parent.view.rect.center, ps.coef, math.sqrt(surplusArea))
    }
  }

}

object FastMover extends LMNtal.Mover {

  import scala.math.{hypot,sqrt}

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

    val force = forceFor(node, params) + parentForce

    node.view.affect(force, elapsedSec)

    val childNodes = if (unyo.core.Env.isMultiCoreEnabled) node.childNodes.par else node.childNodes
    for (n <- childNodes) move(n, elapsedSec, force / node.childNodes.size, params)
  }

  private def resize(node: Node): Unit = {
    for (n <- node.childNodes) resize(n)

    node.attr match {
      case Mem if !node.childNodes.isEmpty => node.view.rect = coverableRect(node)
      case _ =>
    }
  }

  def forceFor(node: Node, params: ForceParams): Point =
    forceOfRepulsion(node, params) +
    forceOfSpring(node, params) +
    forceOfContraction(node, params)

  def forceOfRepulsion(self: Node, params: ForceParams): Point = {
    if (self.isRoot) {
      Point.zero
    } else {
      val ps = params.repulsion
      val selfRect = self.view.rect
      val others = self.parent.childNodes

      var rx = 0.0
      var ry = 0.0
      val sx = selfRect.center.x
      val sy = selfRect.center.y
      for (other <- others) {
        val otherRect = other.view.rect
        val dist = selfRect.distanceWith(otherRect)
        val ox = otherRect.center.x
        val oy = otherRect.center.y
        val dx = sx - ox
        val dy = sy - oy

        if (!(dx.abs < 1e-9 && dy.abs < 1e-9)) {
          val abs = math.hypot(dx, dy)
          val f = ps.coef1 / (dist * dist / ps.coef2 + 1)
          rx += dx * f / abs
          ry += dy * f / abs
        }
      }
      Point(rx, ry)
    }
  }

  def forceOfSpring(self: Node, params: ForceParams): Point = {
    val ps = params.spring
    val selfPoint = self.view.rect.center
    val others = self.neighborNodes

    var rx = 0.0
    var ry = 0.0
    for (other <- self.neighborNodes) {
      val otherPoint = other.view.rect.center
      val dx = otherPoint.x - selfPoint.x
      val dy = otherPoint.y - selfPoint.y
      val abs = math.hypot(dx, dy)
      val f = ps.constant * (abs - ps.length)

      if (!(dx.abs < 1e-9 && dy.abs < 1e-9)) {
        rx += dx / abs * f
        ry += dy / abs * f
      }
    }
    Point(rx, ry)
  }

  def forceOfContraction(self: Node, params: ForceParams): Point = {

    if (self.isRoot) return Point.zero

    val ps = params.contraction
    val sr = self.view.rect
    val sx = sr.center.x
    val sy = sr.center.y

    var rx = 0.0
    var ry = 0.0

    if (!self.parent.isRoot) {
      val parent = self.parent
      val parentSurplusArea = parent.view.rect.area - parent.allChildNodes.size * ps.areaPerNode - ps.threshold
      if (parentSurplusArea > 0) {
        val oc = parent.view.rect.center
        val dx = oc.x - sx
        val dy = oc.y - sy
        if (!(dx.abs < 1e-9 && dy.abs < 1e-9)) {
          val abs = hypot(dx, dy)
          val f = ps.coef * sqrt(abs * sqrt(parentSurplusArea))
          rx += dx / abs * f
          ry += dy / abs * f
        }
      }
    }

    val selfSurplusArea = sr.area - self.allChildNodes.size * ps.areaPerNode - ps.threshold
    if (selfSurplusArea > 0) {
      val ss = sqrt(selfSurplusArea)
      for (other <- self.childNodes) {
        val oc = other.view.rect.center
        val dx = sx - oc.x
        val dy = sy - oc.y
        if (!(dx.abs < 1e-9 && dy.abs < 1e-9)) {
          val abs = hypot(dx, dy)
          val f = ps.coef * sqrt(abs * ss)
          rx -= dx / abs * f
          ry -= dy / abs * f
        }
      }
    }

    Point(rx, ry)
  }

}
