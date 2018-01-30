package graphene.plugin.lmntal

import graphene.plugin.Plugin

class ForceParams {
  val maxForce = 100.0
  object repulsion {
    var coef1 = 80.0
    var coef2 = 1000.0
    override def toString = s"Repulsion(coef1 = $coef1, coef2 = $coef2)"
  }
  object spring {
    var constant = 0.6
    var length = 30.0
    override def toString = s"Spring(constant = $constant, length = $length)"
  }
  object contraction {
    var coef = 0.5
    var threshold = 100.0
    var areaPerNode = 10000.0
    override def toString = s"Contraction(coef = $coef, threshold = $threshold, areaPerNode = $areaPerNode)"
  }

  def deepcopy = {
    val params = new ForceParams

    params.repulsion.coef1 = repulsion.coef1
    params.repulsion.coef2 = repulsion.coef2
    params.spring.constant = spring.constant
    params.spring.length = spring.length
    params.contraction.coef = contraction.coef
    params.contraction.threshold = contraction.threshold
    params.contraction.areaPerNode = contraction.areaPerNode

    params
  }

  override def toString = s"ForceParams(\n  maxForce = $maxForce\n  $repulsion\n  $spring\n  $contraction"

}

class Config {
  var lmntalHome = ""
  var slimPath = ""
  var additionalOptions = ""
  var baseDirectory = ""
  var isProxyVisible = false
  var isDiffAnimationEnabled = false
  var isAutoFocusEnabled = false
  val forces = new ForceParams
}

object LMNtal extends Plugin {

  type GraphType = graphene.model.Graph

  val name = "LMNtal"

  val config = new Config

  def importProperties(properties: java.util.Properties) {
    config.lmntalHome        = properties.getProperty("lmntal_home", System.getenv("LMNTAL_HOME"))
    config.slimPath          = properties.getProperty("slim_path",  config.lmntalHome + "/installed/bin/slim")
    config.baseDirectory     = properties.getProperty("base_directory", "~/")
    config.additionalOptions = properties.getProperty("additional_options", "")
  }

  def exportProperties: java.util.Properties = {
    val properties = new java.util.Properties
    properties.setProperty("lmntal_home", config.lmntalHome)
    properties.setProperty("slim_path", config.slimPath)
    properties.setProperty("base_directory", config.baseDirectory)
    properties.setProperty("additional_options", config.additionalOptions)
    properties
  }

  val source = new LMNtalSource
  val renderer = new DefaultRenderer
  val observer = new graphene.plugin.lmntal.Observer
  val mover = FastMover
  def controlPanel = new ControlPanel(config)
}
