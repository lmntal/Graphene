package unyo.swing

import unyo.util._
import unyo.util.Geometry._

object Graphics {

  implicit class GraphicsExt(val g: java.awt.Graphics) {
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
