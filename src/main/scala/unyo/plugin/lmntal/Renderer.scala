package unyo.plugin.lmntal

import java.awt.{Graphics,Graphics2D,Color,BasicStroke}

import unyo.util._
import unyo.util.Geometry._
import unyo.core.gui.GraphicsContext

trait Renderer {
  implicit class GraphicsExt(val g: Graphics) {
    def drawLine(x1: Double, y1: Double, x2: Double, y2: Double): Unit =
      g.drawLine(x1.toInt, y1.toInt, x2.toInt, y2.toInt)

    def drawLine(p1: Point, p2: Point): Unit = drawLine(p1.x, p1.y, p2.x, p2.y)

    def drawOval(x: Double, y: Double, w: Double, h: Double): Unit = g.drawOval(x.toInt, y.toInt, w.toInt, h.toInt)
    def drawOval(p: Point, d: Dim): Unit = g.drawOval(p.x, p.y, d.width, d.height)
    def drawOval(r: Rect): Unit = g.drawOval(r.point, r.dim)

    def fillOval(x: Double, y: Double, w: Double, h: Double): Unit = g.fillOval(x.toInt, y.toInt, w.toInt, h.toInt)
    def fillOval(p: Point, d: Dim): Unit = fillOval(p.x, p.y, d.width, d.height)
    def fillOval(r: Rect): Unit = fillOval(r.point, r.dim)

    def clearRect(x: Double, y: Double, w: Double, h: Double): Unit = g.clearRect(x.toInt, y.toInt, w.toInt, h.toInt)
    def clearRect(p: Point, d: Dim): Unit = clearRect(p.x, p.y, d.width, d.height)
    def clearRect(r: Rect): Unit = clearRect(r.point, r.dim)

    def drawRect(x: Double, y: Double, w: Double, h: Double): Unit = g.drawRect(x.toInt, y.toInt, w.toInt, h.toInt)
    def drawRect(p: Point, d: Dim): Unit = drawRect(p.x, p.y, d.width, d.height)
    def drawRect(r: Rect): Unit = drawRect(r.point, r.dim)

    def fillRect(x: Double, y: Double, w: Double, h: Double): Unit = g.fillRect(x.toInt, y.toInt, w.toInt, h.toInt)
    def fillRect(p: Point, d: Dim): Unit = fillRect(p.x, p.y, d.width, d.height)
    def fillRect(r: Rect): Unit = fillRect(r.point, r.dim)

    def drawRoundRect(x: Double, y: Double, w: Double, h: Double, aw: Double, ah: Double): Unit =
      g.drawRoundRect(x.toInt, y.toInt, w.toInt, h.toInt, aw.toInt, ah.toInt)
    def drawRoundRect(p: Point, d: Dim, arc: Dim): Unit = drawRoundRect(p.x, p.y, d.width, d.height, arc.width, arc.height)
    def drawRoundRect(r: Rect, arc: Dim): Unit = drawRoundRect(r.point, r.dim, arc)

    def fillRoundRect(x: Double, y: Double, w: Double, h: Double, aw: Double, ah: Double): Unit =
      g.fillRoundRect(x.toInt, y.toInt, w.toInt, h.toInt, aw.toInt, ah.toInt)
    def fillRoundRect(p: Point, d: Dim, arc: Dim): Unit = fillRoundRect(p.x, p.y, d.width, d.height, arc.width, arc.height)
    def fillRoundRect(r: Rect, arc: Dim): Unit = fillRoundRect(r.point, r.dim, arc)

    def drawString(s: String, p: Point) {
      g.drawString(s, p.x.toInt, p.y.toInt)
    }
  }
}

class DefaultRenderer extends LMNtal.Renderer with Renderer {

  import unyo.model._

  var g: Graphics2D = null
  lazy val gctx = unyo.core.gui.MainFrame.instance.mainPanel.graphicsContext
  var graph: Graph = _

  def renderAll(gg: Graphics, graph: Graph): Unit = {
    g = gg.asInstanceOf[Graphics2D];
    this.graph = graph

    renderGrid

    if (graph == null) return

    if (LMNtal.config.isAutoFocusEnabled) gctx.wRect = graph.rootNode.view.rect
    for (node <- graph.rootNode.childNodes) renderEdges(node)
    for (node <- graph.rootNode.childNodes) renderNode(node)
  }

  private def renderGrid: Unit = {
    val bx = gctx.wCenter.x - gctx.wSize.width / 2
    val ex = gctx.wCenter.x + gctx.wSize.width / 2
    val by = gctx.wCenter.y - gctx.wSize.height / 2
    val ey = gctx.wCenter.y + gctx.wSize.height / 2
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

  private val font = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 16)

  private val linkColor = new Color(149, 165, 166)

  private val atomStroke = new BasicStroke(4.0f)
  private val memStroke = new BasicStroke(2.0f)
  private val linkStroke = new BasicStroke(1.5f)

  private val memArc = Dim(6, 6)

  private def renderNode(node: Node): Unit = {
    val view = node.view
    val rect = view.rect

    if (view.willDisappear) {
      val oldPaint = g.getPaint
      g.setPaint(new java.awt.RadialGradientPaint(rect.center.x.toInt, rect.center.y.toInt, 30, Array(0.0f, 1.0f), Array(Color.RED, new Color(255, 255, 255, 0))))
      g.fillOval(rect.pad(Padding(-30, -30, -30, -30)))
      g.setPaint(oldPaint)
    }

    if (view.didAppear) {
      val oldPaint = g.getPaint
      g.setPaint(new java.awt.RadialGradientPaint(rect.center.x.toInt, rect.center.y.toInt, 30, Array(0.7f, 1.0f), Array(Color.GREEN, new Color(255, 255, 255, 0))))
      g.fillOval(rect.pad(Padding(-30, -30, -30, -30)))
      g.setPaint(oldPaint)
    }

    node.attr match {
      case Atom() => {
        g.setFont(font)
        g.setColor(node.view.color)
        g.drawString(node.name, rect.point)

        g.setColor(if (view.selected) Palette.asbestos else Color.WHITE)
        g.fillOval(rect)

        g.setStroke(atomStroke)
        g.setColor(node.view.color)
        g.drawOval(rect)
      }
      case HLAtom() => {
        g.setFont(font)
        g.setColor(node.view.color)
        g.drawString(node.name, rect.point)
        g.fillOval(rect)
      }
      case Mem() => {
        g.setColor(Color.WHITE)
        g.fillRoundRect(rect, memArc)

        g.setStroke(memStroke)
        g.setColor(node.view.color)
        g.drawRoundRect(rect, memArc)
      }
      case _ =>
    }

    for (n <- node.childNodes) renderEdges(n)
    for (n <- node.childNodes) renderNode(n)
  }

  private def renderEdges(node: Node): Unit = {
    g.setColor(linkColor)
    g.setStroke(linkStroke)
    val center = node.view.rect.center
    for (buddy <- node.neighborNodes) g.drawLine(center, buddy.view.rect.center)
  }
}
