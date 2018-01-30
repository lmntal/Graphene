package graphene.plugin.lmntal

import graphene.swing.scalalike._

import graphene.util._
import graphene.util.Geometry._

import graphene.model._

object Observer {

  private def doSmoothColoring(node: Node): Unit = doSmoothColoring(Set(node))
  private def doSmoothColoring(nodes: Set[Node]): Unit = {
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
    // val count = res.values.max
    val count = 6
    for ((n, depth) <- res) {
      n.view.color = Color.fromHSB(0.666f / count * depth, 0.90f, 0.90f)
    }
  }

  private def nodeOptAt(wp: Point): Option[Node] = {
    val graph = LMNtal.source.current
    if (graph == null) None
    else graph.rootNode.allChildNodes.find { n => n.childNodes.isEmpty && n.view.rect.contains(wp) }
  }

}

class Observer extends LMNtal.Observer {

  import java.awt.event.{KeyEvent}
  import java.awt.{Point => JPoint}

  import scala.annotation.tailrec
  import scala.collection.mutable

  private lazy val gctx = graphene.core.gui.MainFrame.instance.mainPanel.graphicsContext

  private val selectedNodes = mutable.Set.empty[Node]
  private var prevPoint: JPoint = null
  private var canMoveNode = false
  private var isMultiSelectionEnabled = false

  val popupMenu = new javax.swing.JPopupMenu {
    import java.awt.event.{ActionListener,ActionEvent}
    import javax.swing.{JMenuItem}

    val item = new JMenuItem("Propagation coloring")
    item.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) = Observer.doSmoothColoring(selectedNodes.toSet)
    })
    add(item)
  }

  private def resetSelection() = {
    for (n <- selectedNodes) n.view.selected = false
    selectedNodes.clear
  }

  def listener: Reactions.Reaction = {
    case MousePressed(source, p, _, _, true) => popupMenu.show(source, p.x, p.y)
    case MousePressed(_, p, _, _, _)  => {
      Observer.nodeOptAt(gctx.worldPointFrom(p)) match {
        case Some(n) => {
          if (!isMultiSelectionEnabled) resetSelection
          n.view.selected = true
          selectedNodes += n
          canMoveNode = true
        }
        case None => resetSelection
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
    case MouseClicked(_, p, _, 2, _)  => for (n <- Observer.nodeOptAt(gctx.worldPointFrom(p))) selectedNodes += n
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
