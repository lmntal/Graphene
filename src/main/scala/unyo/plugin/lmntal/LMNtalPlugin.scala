package unyo.plugin.lmntal

import unyo.plugin.Plugin

object LMNtalPlugin extends Plugin {
  type GraphType = ViewContext

  val renderers = Seq(new DefaultRenderer)
  val observers = Seq(new unyo.plugin.lmntal.Observer)
  val runtimes = Seq(new LMNtalRuntime)
  val movers = Seq(new DefaultMover)
}
