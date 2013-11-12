package unyo.gui

import unyo.utility._
import unyo.Env

class GraphicsContext {

  var sSize = Dim(Env.frameWidth, Env.frameHeight)
  var wCenter = Point(sSize.width / 2, sSize.height / 2)
  var magnificationRate: Double = 1.0 // screen / world

  def wSize = Dim(sSize.width / magnificationRate, sSize.height / magnificationRate)
  def wRect = Rect(
    Point(wCenter.x - wSize.width / 2, wCenter.y - wSize.height / 2),
    wSize
  )

  def worldPointFrom(sp: Point) = Point(
    wCenter.x + (sp.x - sSize.width / 2) / magnificationRate,
    wCenter.y + (sp.y - sSize.height/ 2) / magnificationRate
  )
  def screenPointFrom(wp: Point) = Point(
    (wp.x - wCenter.x) * magnificationRate + sSize.width / 2,
    (wp.y - wCenter.y) * magnificationRate + sSize.height/ 2
  )
  def worldDimFrom(sd: Dim) = Dim(sd.width / magnificationRate, sd.height / magnificationRate)
  def screenDimFrom(wd: Dim) = Dim(wd.width * magnificationRate, wd.height * magnificationRate)
  def worldRectFrom(sr: Rect) = Rect(worldPointFrom(sr.point), worldDimFrom(sr.dim))
  def screenRectFrom(wr: Rect) = Rect(screenPointFrom(wr.point), screenDimFrom(wr.dim))

  def moveBy(sv: Point) {
    wCenter = wCenter + sv / magnificationRate
  }

  def resize(sd: Dim) { sSize = sd }
  def zoom(m: Double, sBase: Point) {
    val sCenter = Point(sSize.width / 2, sSize.height / 2)
    wCenter += (sBase - sCenter) / magnificationRate
    magnificationRate *= m
    magnificationRate = math.min(magnificationRate, 20)
    magnificationRate = math.max(0.05, magnificationRate)
    wCenter -= (sBase - sCenter) / magnificationRate
  }

}
