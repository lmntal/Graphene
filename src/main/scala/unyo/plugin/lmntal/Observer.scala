package unyo.plugin.lmntal

import swing.event.{Event,MousePressed,MouseReleased,MouseDragged}
import swing.event.{Key,KeyPressed,KeyReleased}

import unyo.util._
import unyo.util.Geometry._

class Observer(val runtime: LMNtalPlugin.Runtime) extends LMNtalPlugin.Observer {

  var view: View = null
  var isNodeHandlable = false
  def listenOn(context: unyo.gui.GraphicsContext): swing.Reactions.Reaction = {
    case MousePressed(_, p, _, _, _) => if (isNodeHandlable) {
      val viewContext = runtime.current
      val pos = context.worldPointFrom(p)
      viewContext.viewOptAt(pos) match {
        case Some(v) => view = v
        case None =>
      }
    }
    case MouseReleased(_, p, _, _, _) => if (isNodeHandlable) {
      view = null
    }
    case MouseDragged(_, p, _) => if (isNodeHandlable) {
      if (view != null) {
        val wp = context.worldPointFrom(p)
        view.rect = Rect(wp, view.rect.dim)
      }
    }
    case KeyPressed(_, key, _, _) => if (key == Key.Control) isNodeHandlable = true
    case KeyReleased(_, key, _, _) => if (key == Key.Control) isNodeHandlable = false
    case _ =>
  }

    def canMoveScreen = !isNodeHandlable
}
