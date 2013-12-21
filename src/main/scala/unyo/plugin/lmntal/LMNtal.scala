package unyo.plugin.lmntal

import unyo.plugin.Plugin

class Config {
  var slimPath = ""
  var baseDirectory = ""
  var isProxyVisible = false
  var isDiffAnimationEnabled = false
  object forces {
    val maxForce = 100.0
    object repulsion {
      var coef1 = 80.0
      var coef2 = 1000.0
    }
    object spring {
      var constant = 0.6
      var length = 30.0
    }
    object contraction {
      var coef = 0.5
      var threshold = 100.0
      var areaPerNode = 10000.0
    }
  }
}

object LMNtal extends Plugin {
  type GraphType = unyo.model.Graph

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

  val source = new LMNtalSource
  val renderer = new DefaultRenderer
  val observer = new unyo.plugin.lmntal.Observer
  val mover = new DefaultMover
  def controlPanel = new ControlPanel(config)
}
