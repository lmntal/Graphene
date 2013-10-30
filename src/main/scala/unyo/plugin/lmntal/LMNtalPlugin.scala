package unyo.plugin.lmntal

import unyo.plugin.Plugin

class Config {
  var slimPath = ""
  object forces {
    object replusion {
      var forceBetweenAtoms = 80000.0
      var forceBetweenMems  = 800.0
    }
    object spring {
      var force = 0.6
      var length = 30.0
    }
  }
}

object LMNtalPlugin extends Plugin {
  type GraphType = ViewContext

  val name = "LMNtal"

  val config = new Config

  def importSettings(settings: Map[String,String]) {
    config.slimPath = settings.getOrElse("slim_path", "")
  }

  def exportSettings: Map[String,String] = Map[String,String](
    "slim_path" -> config.slimPath
  )

  val runtimes = Seq(new LMNtalRuntime(config))
  val renderers = Seq(new DefaultRenderer)
  val observers = Seq(new unyo.plugin.lmntal.Observer(runtimes(0)))
  val movers = Seq(new DefaultMover(config))
  val controlPanel = new ControlPanel(config)
}
