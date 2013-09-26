package unyo.gui.renderer

import java.awt.{Graphics,Color}

import unyo.entity.{Graph,Node}

class DefaultRenderer(val g: Graphics, val context: ViewContext) {

  def render(graph: Graph) {
    renderRoot(graph)
  }

  def renderRoot(graph: Graph) {
    for (node <- graph.nodes) {
      renderNode(node)
    }
  }

  def renderNode(node: Node) {
    renderEdges(node)
    val viewNode = context.viewNodeOf(node)
    g.fillOval(
      viewNode.x.toInt - 20,
      viewNode.y.toInt - 20,
      40,
      40
    )
  }

  def renderEdges(node: Node) {
    val view1 = context.viewNodeOf(node)
    for (i <- 0 until node.arity) {
      val buddy = node.buddyAt(i)
      val view2 = context.viewNodeOf(buddy)

      g.drawLine(
        view1.x.toInt,
        view1.y.toInt,
        view2.x.toInt,
        view2.y.toInt
      )
    }
  }
}
