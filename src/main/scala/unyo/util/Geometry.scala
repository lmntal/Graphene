package unyo.util

object Geometry {
  implicit def toUnyoDimension(d: java.awt.Dimension) = Dimension(d.width, d.height)
  implicit def toAWTDimension(d: Dimension) = new java.awt.Dimension(d.width.toInt, d.height.toInt)
}

object Dimension {
  def apply(w: Double, h: Double) = new Dimension(w, h)
}

case class Point(x: Double, y: Double) {
  def +(other: Point) = Point(x + other.x, y + other.y)
  def -(other: Point) = Point(x - other.x, y - other.y)
  def *(other: Double) = Point(x * other, y * other)
  def /(other: Double) = Point(x / other, y / other)
  def dot(other: Point) = x*other.x + y*other.y
  def sqabs: Double = dot(this)
  def abs: Double = math.sqrt(sqabs)
  def unit: Point = this / abs
}

class Dimension(val width: Double, val height: Double)
