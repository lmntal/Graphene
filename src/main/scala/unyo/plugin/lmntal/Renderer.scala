package unyo.plugin.lmntal

import java.awt.{Graphics,Graphics2D,Color}

import unyo.utility._
import unyo.utility.Geometry._
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
    def clearRect(r: Rect) {
      g.clearRect(r.point.x.toInt, r.point.y.toInt, r.dim.width.toInt, r.dim.height.toInt)
    }
    def drawRect(r: Rect) {
      g.drawRect(r.point.x.toInt, r.point.y.toInt, r.dim.width.toInt, r.dim.height.toInt)
    }
    def fillRect(r: Rect) {
      g.fillRect(r.point.x.toInt, r.point.y.toInt, r.dim.width.toInt, r.dim.height.toInt)
    }

    def drawString(s: String, p: Point) {
      g.drawString(s, p.x.toInt, p.y.toInt)
    }
  }
}

class DefaultRenderer extends LMNtalPlugin.Renderer with Renderer {

  var g: Graphics2D = null
  var context: GraphicsContext = null
  var graph: ViewContext = null
  def renderAll(gg: Graphics, context: GraphicsContext, graph: ViewContext) {
    g = gg.asInstanceOf[Graphics2D];
    this.context = context
    this.graph = graph

    g.clearRect(Rect(Point(0,0), context.sSize))

    g.translate(context.sSize.width/2, context.sSize.height/2)
    g.scale(context.magnificationRate, context.magnificationRate)
    g.translate(-context.wCenter.x, -context.wCenter.y)

    renderGrid

    if (graph == null) return
    if (graph.rootMem == null) return

    for (mem <- graph.rootMem.mems) renderMem(mem)
    for (atom <- graph.rootMem.atoms) renderEdges(atom)
    for (atom <- graph.rootMem.atoms) renderAtom(atom)
  }

  def renderGrid {
    val bx = context.wCenter.x - context.wSize.width / 2
    val ex = context.wCenter.x + context.wSize.width / 2
    val by = context.wCenter.y - context.wSize.height / 2
    val ey = context.wCenter.y + context.wSize.height / 2
    g.setColor(new Color(127, 140, 141))
    for (x <- (bx.toInt/100*100).to(ex.toInt/100*100, 100)) {
      val p1 = Point(x, by)
      val p2 = Point(x, ey)
      g.drawLine(p1, p2)
    }

    for (y <- (by.toInt/100*100).to(ey.toInt/100*100, 100)) {
      val p1 = Point(bx, y)
      val p2 = Point(ex, y)
      g.drawLine(p1, p2)
    }
  }

  def renderMem(mem: Mem) {
    val viewNode = graph.viewOf(mem)
    g.setColor(new Color(52, 152, 219))
    g.fillRect(viewNode.rect)
    g.setColor(Color.WHITE)
    g.fillRect(viewNode.rect.pad(Padding(2, 2, 2, 2)))

    for (submem <- mem.mems) renderMem(submem)
    for (node <- mem.atoms) renderEdges(node)
    for (node <- mem.atoms) renderAtom(node)
  }

  def renderAtom(atom: Atom) {
    if (atom.isProxy) return

    val viewNode = graph.viewOf(atom)
    val rect = viewNode.rect
    g.setFont(new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 16))
    g.setColor(new Color(52, 152, 219))
    g.drawString(atom.name, rect.point)
    g.fillOval(rect)
    g.setColor(Color.WHITE)
    g.fillOval(rect.pad(Padding(3, 3, 3, 3)))
  }

  def renderEdges(atom: Atom) {
    if (atom.isProxy) return

    val view1 = graph.viewOf(atom)
    g.setColor(new Color(41, 128, 185))
    for (i <- 0 until atom.arity) {
      var buddy = atom.buddyAt(i)
      val view2 = graph.viewOf(buddy)

      g.drawLine(
        view1.rect.center,
        view2.rect.center
      )
    }
  }
}
