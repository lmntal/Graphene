package unyo.plugin.lmntal

import swing.event.{Event}

class Observer extends LMNtalPlugin.Observer {

  val eventDispatched: swing.Reactions.Reaction = {
    case swing.event.MouseDragged(_, p, _) =>
    case _ =>
  }

}
