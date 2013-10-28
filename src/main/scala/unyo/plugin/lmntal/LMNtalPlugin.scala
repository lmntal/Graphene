package unyo.plugin.lmntal

import unyo.plugin.Plugin

class Config {
  var slimPath = "/Users/charlie/Documents/slim/slim/src/slim"
  object forces {
    object replusion {
      var forceBetweenAtoms = 1000000.0
      var forceBetweenMems  = 10000.0
    }
    object spring {
      var force = 2.0
      var length = 120.0
    }
  }
}

object LMNtalPlugin extends Plugin {
  type GraphType = ViewContext

  val config = new Config

  val runtimes = Seq(new LMNtalRuntime(config))
  val renderers = Seq(new DefaultRenderer)
  val observers = Seq(new unyo.plugin.lmntal.Observer(runtimes(0)))
  val movers = Seq(new DefaultMover(config))
  val controlPanel = new ControlPanel(config)
}
