package unyo.core.gui

import unyo.util._
import unyo.core.Env

class GraphicsContext {

  var sSize = Dim(Env.frameWidth, Env.frameHeight)
  var wCenter = Point(sSize.width / 2, sSize.height / 2)
  var magnificationRate: Double = 1.0 // screen / world

  def wSize = Dim(sSize.width / magnificationRate, sSize.height / magnificationRate)
  def wRect = Rect(wCenter - wSize.toPoint / 2, wSize)
  def wRect_=(r: Rect) = {
    wCenter = r.center
    magnificationRate = math.min(sSize.width / r.dim.width, sSize.height / r.dim.height)
  }

  def worldPointFrom(sp: Point) = wCenter + (sp - sSize.toPoint / 2) / magnificationRate
  def screenPointFrom(wp: Point) = (wp - wCenter) * magnificationRate + sSize.toPoint / 2

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
