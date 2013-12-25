package unyo.plugin.lmntal

import unyo.swing.scalalike._

import unyo.util._
import unyo.util.Geometry._

import unyo.model._

class Observer extends LMNtal.Observer {

  import java.awt.event.{KeyEvent}

  private def viewOptAt(wp: Point): Option[View] = {
    val graph = LMNtal.source.current
    graph.rootNode.allChildNodes.filter(_.childNodes.isEmpty).find(_.view.rect.contains(wp)).map(_.view)
  }

  var view: View = null
  var isNodeHandlable = false
  lazy val graphicsContext = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext
  var prevPoint: java.awt.Point = null

  def listener: Reactions.Reaction = {
    case MousePressed(_, p, _, _, _) =>
      if (isNodeHandlable) {
        val pos = graphicsContext.worldPointFrom(p)
        for (v <- viewOptAt(pos)) { view = v; v.fixed = true }
      } else {
        prevPoint = p
      }
    case MouseReleased(_, p, _, _, _) =>
      if (isNodeHandlable) {
        view.fixed = false
        view = null
      } else {
        prevPoint = null
      }
    case MouseDragged(_, p, _) =>
      if (isNodeHandlable) {
        if (view != null) {
          val wp = graphicsContext.worldPointFrom(p)
          view.rect = Rect(wp, view.rect.dim)
        }
      } else if (prevPoint != null) {
        graphicsContext.moveBy(prevPoint - p)
        prevPoint = p
      }
    case MouseWheelMoved(_, p, _, rot) => graphicsContext.zoom(math.pow(1.01, rot), p)
    case KeyPressed(_, key, _, _) => if (key == KeyEvent.VK_Z) isNodeHandlable = true
    case KeyReleased(_, key, _, _) => if (key == KeyEvent.VK_Z) isNodeHandlable = false
    case _ =>
  }
}
