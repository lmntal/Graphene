package unyo.plugin.lmntal

import unyo.swing.scalalike._

import unyo.util._
import unyo.util.Geometry._

import unyo.model._

class Observer extends LMNtal.Observer {

  import java.awt.event.{KeyEvent}
  import java.awt.{Point => JPoint}

  private def viewOptAt(wp: Point): Option[View] = {
    val graph = LMNtal.source.current
    graph.rootNode.allChildNodes.filter(_.childNodes.isEmpty).find(_.view.rect.contains(wp)).map(_.view)
  }

  var isNodeHandlable = false
  lazy val gctx = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext

  var view: View = null
  def viewPressed(p: JPoint): Unit = for (v <- viewOptAt(gctx.worldPointFrom(p))) { view = v; v.fixed = true }
  def viewDragged(p: JPoint): Unit = if (view != null) view.rect = Rect(gctx.worldPointFrom(p), view.rect.dim)
  def viewReleased(p: JPoint): Unit = if (view != null) { view.fixed = false; view = null }

  var prevPoint: JPoint = null
  def screenPressed(p: JPoint): Unit = prevPoint = p
  def screenDragged(p: JPoint): Unit = if (prevPoint != null) { gctx.moveBy(prevPoint - p); prevPoint = p }
  def screenReleased(p: JPoint): Unit = prevPoint = null

  def listener: Reactions.Reaction = {
    case MousePressed(_, p, _, _, _)  => if (isNodeHandlable) viewPressed(p)  else screenPressed(p)
    case MouseReleased(_, p, _, _, _) => if (isNodeHandlable) viewReleased(p) else screenReleased(p)
    case MouseDragged(_, p, _)        => if (isNodeHandlable) viewDragged(p)  else screenDragged(p)
    case MouseWheelMoved(_, p, _, rot) => gctx.zoom(math.pow(1.01, rot), p)
    case KeyPressed(_, key, _, _) => if (key == KeyEvent.VK_Z) isNodeHandlable = !isNodeHandlable
    case _ =>
  }
}
