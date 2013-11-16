package unyo.plugin.lmntal

import unyo.plugin.Plugin

class Config {
  var slimPath = ""
  var baseDirectory = ""
  var isProxyVisible = false
  object forces {
    object repulsion {
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

  def importProperties(properties: java.util.Properties) {
    config.slimPath      = properties.getProperty("slim_path", "")
    config.baseDirectory = properties.getProperty("base_directory", "~/")
  }

  def exportProperties: java.util.Properties = {
    val properties = new java.util.Properties
    properties.setProperty("slim_path", config.slimPath)
    properties.setProperty("base_directory", config.baseDirectory)
    properties
  }

  val runtime = new LMNtalRuntime
  val renderer = new DefaultRenderer
  val observer = new unyo.plugin.lmntal.Observer
  val mover = new DefaultMover
  def controlPanel = new ControlPanel(config)
}
