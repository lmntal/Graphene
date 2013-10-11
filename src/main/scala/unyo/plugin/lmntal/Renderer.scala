package unyo.plugin.lmntal

import java.awt.{Graphics,Color}

import unyo.util._
import unyo.util.Geometry._
import unyo.gui.GraphicsContext

trait Renderer {
  implicit class GraphicsExt(val g: Graphics) {
    def drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
      g.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)
    }
    def drawLine(p1: Point, p2: Point) {
      drawLine(p1.x, p1.y, p2.x, p2.y)
    }

    def fillOval(x: Double, y: Double, w: Double, h: Double) {
      g.fillOval(x.toInt, y.toInt, w.toInt, h.toInt)
    }
    def fillOval(p: Point, w: Double, h: Double) {
      fillOval(p.x, p.y, w, h)
    }
    def fillOval(p: Point, d: Dim) {
      fillOval(p, d.width, d.height)
    }
    def fillOval(r: Rect) {
      fillOval(r.point, r.dim)
    }
    def drawRect(r: Rect) {
      g.drawRect(r.point.x.toInt, r.point.y.toInt, r.dim.width.toInt, r.dim.height.toInt)
    }
    def fillRect(r: Rect) {
      g.fillRect(r.point.x.toInt, r.point.y.toInt, r.dim.width.toInt, r.dim.height.toInt)
    }
  }
}

class DefaultRenderer extends LMNtalPlugin.Renderer with Renderer {

  var g: Graphics = null
  var context: GraphicsContext = null
  def renderAll(g: Graphics, context: GraphicsContext, graph: ViewContext) {
    this.g = g
    this.context = context

    g.clearRect(0, 0, 2000, 2000)
    renderGrid

    if (graph != null) render(graph)
  }

  var viewContext: ViewContext = null
  def render(graph: ViewContext) {
    viewContext = graph
    renderRoot(graph.rootMem)
  }

  def renderGrid {
    val bx = context.wCenter.x - context.wSize.width / 2
    val ex = context.wCenter.x + context.wSize.width / 2
    val by = context.wCenter.y - context.wSize.height / 2
    val ey = context.wCenter.y + context.wSize.height / 2
    g.setColor(new Color(127, 140, 141))
    for (x <- (bx.toInt/100*100).to(ex.toInt/100*100, 100)) {
      val p1 = context.screenPointFrom(Point(x, by))
      val p2 = context.screenPointFrom(Point(x, ey))
      g.drawLine(p1, p2)
    }

    for (y <- (by.toInt/100*100).to(ey.toInt/100*100, 100)) {
      val p1 = context.screenPointFrom(Point(bx, y))
      val p2 = context.screenPointFrom(Point(ex, y))
      g.drawLine(p1, p2)
    }
  }

  def renderRoot(graph: Mem) {
    if (graph == null) return
    renderMem(graph)
  }

  def renderMem(graph: Mem) {
    val viewNode = viewContext.viewOf(graph)
    g.setColor(new Color(52, 152, 219))
    g.fillRect(context.screenRectFrom(viewNode.rect))
    g.setColor(Color.WHITE)
    g.fillRect(context.screenRectFrom(viewNode.rect).pad(Padding(2, 2, 2, 2)))

    for (subgraph <- graph.mems) renderMem(subgraph)
    for (node <- graph.atoms) renderEdges(node)
    for (node <- graph.atoms) renderAtom(node)
  }

  def renderAtom(node: Atom) {
    val viewNode = viewContext.viewOf(node)
    g.setColor(new Color(52, 152, 219))
    g.fillOval(context.screenRectFrom(viewNode.rect))
    g.setColor(Color.WHITE)
    g.fillOval(context.screenRectFrom(viewNode.rect).pad(Padding(3, 3, 3, 3)))
  }

  def renderEdges(node: Atom) {
    val view1 = viewContext.viewOf(node)
    g.setColor(new Color(41, 128, 185))
    for (i <- 0 until node.arity) {
      val buddy = node.buddyAt(i)
      val view2 = viewContext.viewOf(buddy)

      g.drawLine(
        context.screenPointFrom(view1.rect.center),
        context.screenPointFrom(view2.rect.center)
      )
    }
  }
}

class ViewContext() {
  private val viewNodeFromID = collection.mutable.Map.empty[Int, View]
  val r = new util.Random
  def viewOf(node: Atom): View = {
    viewNodeFromID.getOrElseUpdate(node.id, new View(Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(40, 40))))
  }
  def viewOf(graph: Mem): View = {
    viewNodeFromID.getOrElseUpdate(graph.id, {
      new View(coverableRect(graph))
    })
  }
  def coverableRect(g: Mem): Rect = {
    val rects = g.mems.map(viewOf(_).rect) ++ g.atoms.map(viewOf(_).rect)
    if (rects.isEmpty) Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(80, 80)) else rects.reduceLeft(_ << _)
  }

  var rootMem: Mem = null
  def rewrite(g: Mem) {
    rootMem = g
    updateMem(rootMem)
  }
  private def updateMem(g: Mem) {
    for (node <- g.atoms) viewOf(node)
    for (graph <- g.mems) updateMem(graph)
  }
}

class View(var rect: Rect) {
  var speed = Point(0, 0)

  val mass = 10.0
  val decayRate = 0.90
  def force(f: Point, elapsed: Double) {
    speed = (speed + f * elapsed / mass) * decayRate
    rect = Rect(rect.point + speed * elapsed, rect.dim)
  }
}

