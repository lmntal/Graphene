package unyo.plugin

trait Plugin {
  type GraphType

  def name: String

  def importSettings(settings: Map[String,String]): Unit
  def exportSettings: Map[String,String]

  def renderers: Seq[Renderer]
  def observers: Seq[Observer]
  def runtimes: Seq[Runtime]
  def movers: Seq[Mover]
  def controlPanel: javax.swing.JPanel

  trait Renderer {
    def renderAll(g: java.awt.Graphics, context: unyo.gui.GraphicsContext, graph: GraphType)
  }

  trait Observer {
    def listenOn(context: unyo.gui.GraphicsContext): unyo.swing.scalalike.Reactions.Reaction
    def canMoveScreen: Boolean
  }

  trait Runtime {
    def exec(options: Seq[String]): GraphType
    def current: GraphType
    def next: GraphType
    def hasNext: Boolean
  }

  trait Mover {
    def moveAll(graph: GraphType, elapsed: Double)
  }

}


