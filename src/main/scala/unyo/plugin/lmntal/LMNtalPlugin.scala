package unyo.plugin.lmntal

import unyo.plugin.Plugin

class Forces {
  object replusion {
    var forceBetweenAtoms = 1000000.0
    var forceBetweenMems  = 10000.0
  }
  object spring {
    var force = 2.0
    var length = 120.0
  }
}

object LMNtalPlugin extends Plugin {
  type GraphType = ViewContext

  val forces = new Forces

  val runtimes = Seq(new LMNtalRuntime)
  val renderers = Seq(new DefaultRenderer)
  val observers = Seq(new unyo.plugin.lmntal.Observer(runtimes(0)))
  val movers = Seq(new DefaultMover(forces))
  val controlPanel = new ControlPanel
}
