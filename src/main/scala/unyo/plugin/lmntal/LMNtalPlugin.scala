package unyo.plugin.lmntal

import unyo.plugin.Plugin

class Config {
  var slimPath = ""
  var baseDirectory = ""
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
    config.slimPath      = settings.getOrElse("slim_path", "")
    config.baseDirectory = settings.getOrElse("base_directory", "~/")
  }

  def exportSettings: Map[String,String] = Map[String,String](
    "slim_path" -> config.slimPath,
    "base_directory" -> config.baseDirectory
  )

  def runtimes = Seq(new LMNtalRuntime(config))
  def renderers = Seq(new DefaultRenderer)
  def observers = Seq(new unyo.plugin.lmntal.Observer(runtimes(0)))
  def movers = Seq(new DefaultMover(config))
  def controlPanel = new ControlPanel(config)
}
