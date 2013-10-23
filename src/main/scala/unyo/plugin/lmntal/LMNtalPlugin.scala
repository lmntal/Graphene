package unyo.plugin.lmntal

import unyo.plugin.Plugin

object LMNtalPlugin extends Plugin {
  type GraphType = ViewContext

  val runtimes = Seq(new LMNtalRuntime)
  val renderers = Seq(new DefaultRenderer)
  val observers = Seq(new unyo.plugin.lmntal.Observer(runtimes(0)))
  val movers = Seq(new DefaultMover)
  val controlPanel = new ControlPanel
}
