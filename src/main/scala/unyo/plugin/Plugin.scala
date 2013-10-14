package unyo.plugin

trait Plugin {
  type GraphType

  def renderers: Seq[Renderer]
  def observers: Seq[Observer]
  def runtimes: Seq[Runtime]
  def movers: Seq[Mover]

  trait Renderer {
    def renderAll(g: java.awt.Graphics, context: unyo.gui.GraphicsContext, graph: GraphType)
  }

  trait Observer {
    def eventDispatched: swing.Reactions.Reaction
  }

  trait Runtime {
    def exec(options: Seq[String]): GraphType
    def next: GraphType
    def hasNext: Boolean
  }

  trait Mover {
    def moveAll(graph: GraphType, elapsed: Double)
  }
}


