package unyo.plugin

trait Plugin {
  type GraphType

  def renderers: Seq[Renderer]
  def runtimes: Seq[Runtime]
  def movers: Seq[Mover]

  trait Renderer {
    def render(graph: GraphType)
  }

  abstract class Runtime(val options: Seq[String]) {
    def next: GraphType
    def hasNext: Boolean
  }

  trait Mover {
    def moveAll(graph: GraphType, elapsed: Double)
  }
}


