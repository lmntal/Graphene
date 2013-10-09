package unyo.plugin.lmntal

import unyo.plugin.Plugin

object LMNtalPlugin extends Plugin {
  type GraphType = VisualGraph

  val renderers = Seq.empty[Renderer]
  val runtimes = Seq.empty[Runtime]
  val movers = Seq.empty[Mover]
}
