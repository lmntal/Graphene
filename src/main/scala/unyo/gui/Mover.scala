package unyo.gui

import unyo.entity.{Graph,Node}
import unyo.util._

trait Mover {
}

class DefaultMover extends Mover {

  var visualGraph: VisualGraph = null
  def move(visualGraph: VisualGraph, elapsedSec: Double) {
    if (visualGraph == null || visualGraph.graph == null) return
    this.visualGraph = visualGraph
    move(visualGraph.graph, elapsedSec)
  }

  def move(graph: Graph, elapsedSec: Double) {
    val allNodes = allNodesOf(graph)
    for (node <- graph.nodes) {
      val v1 = visualGraph.viewNodeOf(node)
      var vec = Point(0, 0)

      vec = vec + forceOfReplusion(node, allNodes)
      vec = vec + forceOfSpring(node)


      v1.force(vec, elapsedSec)

    }
  }

  private def forceOfReplusion(self: Node, allNodes: collection.Set[_ <: Node]): Point = {
    var vec = Point(0, 0)
    val v1 = visualGraph.viewNodeOf(self)
    for (other <- allNodes) {
      if (self != other) {
        val v2 = visualGraph.viewNodeOf(other)
        val d = v2.pos - v1.pos
        val f = 1000000.0 / d.sqabs
        vec = vec - d.unit * f
      }
    }
    vec
  }

  private def forceOfSpring(self: Node): Point = {
    var vec = Point(0, 0)
    val v1 = visualGraph.viewNodeOf(self)
    for (i <- 0 until self.arity) {
      val other = self.buddyAt(i)
      val v2 = visualGraph.viewNodeOf(other)
      val d = v2.pos - v1.pos
      val f = 2.0 * (d.abs - 120)
      vec = vec + d.unit * f
    }
    vec
  }

  private def allNodesOf(graph: Graph): collection.Set[_ <: Node] = {
    val lhs: collection.Set[_ <: Node] = graph.nodes
    val rhs: collection.Set[_ <: Node] = graph.graphs.map(allNodesOf _).flatten
    lhs ++ rhs
  }

}