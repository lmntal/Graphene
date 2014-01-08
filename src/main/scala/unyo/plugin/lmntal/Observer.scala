package unyo.plugin.lmntal

import unyo.swing.scalalike._

import unyo.util._
import unyo.util.Geometry._

import unyo.model._

class Observer extends LMNtal.Observer {

  import java.awt.event.{KeyEvent}
  import java.awt.{Point => JPoint}

  import scala.annotation.tailrec
  import scala.collection.mutable

  private def nodeOptAt(wp: Point): Option[Node] = {
    val graph = LMNtal.source.current
    if (graph == null) None
    else graph.rootNode.allChildNodes.find { n => n.childNodes.isEmpty && n.view.rect.contains(wp) }
  }

  var canMoveNode = false
  lazy val gctx = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext

  def selectNode(n: Node) = {
    n.view.selected = true
  }

  def unselectNode(n: Node) = {
    n.view.selected = false
  }

  val selectedNodes = mutable.Set.empty[Node]
  var prevPoint: JPoint = null
  def nodePressed(p: JPoint): Unit = nodeOptAt(gctx.worldPointFrom(p)) match {
    case Some(n) => {
      if (!isMultiSelectionEnabled) {
        for (n <- selectedNodes) unselectNode(n)
        selectedNodes.clear
      }
      selectNode(n)
      selectedNodes += n
    }
    case None => {
      for (n <- selectedNodes) unselectNode(n)
      selectedNodes.clear
    }
  }
  def nodeDragged(p: JPoint): Unit = for (n <- selectedNodes) {
    n.view.rect = n.view.rect.movedBy(gctx.worldPointFrom(p) - gctx.worldPointFrom(prevPoint))
  }
  def nodeReleased(p: JPoint): Unit = {}

  def screenPressed(p: JPoint): Unit = {}
  def screenDragged(p: JPoint): Unit = if (prevPoint != null) gctx.moveBy(prevPoint - p)
  def screenReleased(p: JPoint): Unit = {}

  def doLinearColoring(node: Node): Unit = doLinearColoring(Set(node))
  def doLinearColoring(nodes: Set[Node]): Unit = {
    if (nodes.isEmpty) return

    def searchDepth(bases: Set[Node]): Map[Node, Int] = {
      @scala.annotation.tailrec
      def _searchDepth(bases: Set[Node], touredIDs: Set[ID], depth: Int, result: Map[Node, Int]): Map[Node, Int] =
        if (bases.isEmpty) result
        else _searchDepth(
               bases.flatMap { n => n.neighborNodes }.filter { n => !touredIDs.contains(n.id) },
               touredIDs ++ bases.map { _.id },
               depth + 1,
               bases.map { n => (n, depth) }.toMap ++ result
             )
      _searchDepth(bases, Set.empty[ID], 0, Map.empty[Node, Int])
    }

    val res = searchDepth(nodes.toSet)
    val count = res.values.max
    for ((n, depth) <- res) {
      n.view.color = Color.fromHSB(0.666f / count * depth, 0.90f, 0.90f)
    }
  }

  var isMultiSelectionEnabled = false
  def listener: Reactions.Reaction = {
    case MousePressed(_, p, _, _, _)  => {
      nodeOptAt(gctx.worldPointFrom(p)) match {
        case Some(n) => {
          if (!isMultiSelectionEnabled) {
            for (n <- selectedNodes) unselectNode(n)
            selectedNodes.clear
          }
          selectNode(n)
          selectedNodes += n
          canMoveNode = true
        }
        case None => {
          for (n <- selectedNodes) unselectNode(n)
          selectedNodes.clear
        }
      }
      prevPoint = p
    }
    case MouseReleased(_, p, _, _, _) => {
      prevPoint = null
      canMoveNode = false
    }
    case MouseDragged(_, p, _)        => {
      if (prevPoint != null) {
        if (canMoveNode) for (n <- selectedNodes) n.view.rect = n.view.rect.movedBy(gctx.worldPointFrom(p) - gctx.worldPointFrom(prevPoint))
        else gctx.moveBy(prevPoint - p)
      }
      prevPoint = p
    }
    case MouseClicked(_, p, _, 2, _)  => for (n <- nodeOptAt(gctx.worldPointFrom(p))) selectedNodes += n
    case MouseWheelMoved(_, p, _, rot) => gctx.zoom(math.pow(1.01, rot), p)
    case KeyPressed(_, key, _, _) => key match {
      case KeyEvent.VK_SHIFT => isMultiSelectionEnabled = true
      case _ =>
    }
    case KeyReleased(_, key, _, _) => key match {
      case KeyEvent.VK_SHIFT => isMultiSelectionEnabled = false
      case _ =>
    }
    case _ =>
  }
}
