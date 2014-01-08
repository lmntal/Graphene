package unyo.plugin.lmntal

import unyo.swing.scalalike._

import unyo.util._
import unyo.util.Geometry._

import unyo.model._

class Observer extends LMNtal.Observer {

  import java.awt.event.{KeyEvent}
  import java.awt.{Point => JPoint}

  import scala.annotation.tailrec

  private def nodeOptAt(wp: Point): Option[Node] = {
    val graph = LMNtal.source.current
    graph.rootNode.allChildNodes.find { n => n.childNodes.isEmpty && n.view.rect.contains(wp) }
  }

  var isNodeHandlable = false
  lazy val gctx = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext

  var view: View = null
  def viewPressed(p: JPoint): Unit = for (n <- nodeOptAt(gctx.worldPointFrom(p))) { view = n.view; n.view.fixed = true }
  def viewDragged(p: JPoint): Unit = if (view != null) view.rect = Rect(gctx.worldPointFrom(p), view.rect.dim)
  def viewReleased(p: JPoint): Unit = if (view != null) { view.fixed = false; view = null }

  var prevPoint: JPoint = null
  def screenPressed(p: JPoint): Unit = prevPoint = p
  def screenDragged(p: JPoint): Unit = if (prevPoint != null) { gctx.moveBy(prevPoint - p); prevPoint = p }
  def screenReleased(p: JPoint): Unit = prevPoint = null

  def doLinearColoring(node: Node): Unit = doLinearColoring(Seq(node))
  def doLinearColoring(nodes: Seq[Node]): Unit = {
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

  var selectingMode = false
  val selectedNodes = collection.mutable.ArrayBuffer.empty[Node]
  def listener: Reactions.Reaction = {
    case MousePressed(_, p, _, _, _)  => if (isNodeHandlable) viewPressed(p)  else screenPressed(p)
    case MouseReleased(_, p, _, _, _) => if (isNodeHandlable) viewReleased(p) else screenReleased(p)
    case MouseDragged(_, p, _)        => if (isNodeHandlable) viewDragged(p)  else screenDragged(p)
    case MouseClicked(_, p, _, 2, _)  => for (n <- nodeOptAt(gctx.worldPointFrom(p))) selectedNodes += n
    case MouseWheelMoved(_, p, _, rot) => gctx.zoom(math.pow(1.01, rot), p)
    case KeyPressed(_, key, _, _) => key match {
      case KeyEvent.VK_Z => isNodeHandlable = !isNodeHandlable
      case KeyEvent.VK_SHIFT => { selectingMode = true; }
      case _ =>
    }
    case KeyReleased(_, key, _, _) => key match {
      case KeyEvent.VK_SHIFT => { selectingMode = false; doLinearColoring(selectedNodes); selectedNodes.clear }
      case _ =>
    }
    case _ =>
  }
}
