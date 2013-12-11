package unyo.plugin.lmntal

import unyo.swing.scalalike._

import unyo.util._
import unyo.util.Geometry._

class Observer extends LMNtalPlugin.Observer {

  import java.awt.event.{KeyEvent}

  var view: View = null
  var isNodeHandlable = false
  def listenOn(context: unyo.core.gui.GraphicsContext): Reactions.Reaction = {
    case MousePressed(_, p, _, _, _) => if (isNodeHandlable) {
      val viewContext = LMNtalPlugin.runtime.current
      val pos = context.worldPointFrom(p)
      viewContext.viewOptAt(pos) match {
        case Some(v) => { view = v; v.fixed = true }
        case None =>
      }
    }
    case MouseReleased(_, p, _, _, _) => if (isNodeHandlable) {
      view.fixed = false
      view = null
    }
    case MouseDragged(_, p, _) => if (isNodeHandlable) {
      if (view != null) {
        val wp = context.worldPointFrom(p)
        view.rect = Rect(wp, view.rect.dim)
      }
    }
    case KeyPressed(_, key, _, _) => if (key == KeyEvent.VK_Z) isNodeHandlable = true
    case KeyReleased(_, key, _, _) => if (key == KeyEvent.VK_Z) isNodeHandlable = false
    case _ =>
  }

    def canMoveScreen = !isNodeHandlable
}
