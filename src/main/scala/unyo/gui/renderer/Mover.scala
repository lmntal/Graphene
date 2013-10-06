package unyo.gui.renderer

import unyo.entity.{Graph,Node}
import unyo.util._

trait Mover {
}

class DefaultMover extends Mover {
  
  var visualGraph: VisualGraph = null
  def move(visualGraph: VisualGraph) {
    if (visualGraph == null || visualGraph.graph == null) return
    this.visualGraph = visualGraph
    move(visualGraph.graph)
  }

  def move(graph: Graph) {
    for (node1 <- graph.nodes) {
      val v1 = visualGraph.viewNodeOf(node1)
      var vec = Point(0, 0)
      for (node2 <- allNodes(graph)) {
        if (node1 != node2) {
          val v2 = visualGraph.viewNodeOf(node2)
          val d = v2.pos - v1.pos
          val f = 1000000.0 / d.sqabs
          vec = vec + d.unit * f
        }
      }

      for (i <- 0 until node1.arity) {
        val v2 = visualGraph.viewNodeOf(node1.buddyAt(i))
        val d = v2.pos - v1.pos
        val f = 200.0 * (d.abs - 120)
        vec = vec + d.unit * f
      }

      v1.force(vec, 0.01)

    }
  }

  private def allNodes(graph: Graph): collection.Set[_ <: Node] = {
    val lhs: collection.Set[_ <: Node] = graph.nodes
    val rhs: collection.Set[_ <: Node] = graph.graphs.map(allNodes _).flatten
    lhs ++ rhs
}

}
