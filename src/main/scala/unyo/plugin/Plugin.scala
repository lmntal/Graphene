package unyo.plugin

trait Plugin {
  type GraphType

  def name: String

  def importProperties(settings: java.util.Properties): Unit
  def exportProperties: java.util.Properties

  def renderer: Renderer
  def observer: Observer
  def source: Source
  def mover: Mover
  def controlPanel: javax.swing.JPanel

  trait Renderer {
    def renderAll(g: java.awt.Graphics, context: unyo.core.gui.GraphicsContext, graph: GraphType)
  }

  trait Observer {
    def listenOn(context: unyo.core.gui.GraphicsContext): unyo.swing.scalalike.Reactions.Reaction
    def canMoveScreen: Boolean
  }

  trait Source {
    def run(options: Seq[String]): GraphType
    def current: GraphType
    def next: GraphType
    def hasNext: Boolean
  }

  trait Mover {
    def moveAll(graph: GraphType, elapsed: Double)
  }

}


