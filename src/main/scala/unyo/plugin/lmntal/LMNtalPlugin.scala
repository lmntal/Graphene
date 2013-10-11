package unyo.plugin.lmntal

import unyo.plugin.Plugin

object LMNtalPlugin extends Plugin {
  type GraphType = ViewContext

  val renderers = Seq(new DefaultRenderer)
  val runtimes = Seq(new LMNtalRuntime)
  val movers = Seq(new DefaultMover)
}
