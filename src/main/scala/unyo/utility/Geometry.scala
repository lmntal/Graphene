package unyo.utility

object Geometry {
  import scala.language.implicitConversions

  implicit def toUnyoDimension(d: java.awt.Dimension) = Dim(d.width, d.height)
  implicit def toAWTDimension(d: Dim) = new java.awt.Dimension(d.width.toInt, d.height.toInt)

  implicit def toUnyoPoint(p: java.awt.Point) = Point(p.x, p.y)
  implicit def toAWTPoint(p: Point) = new java.awt.Point(p.x.toInt, p.y.toInt)
}

case class Dim(width: Double, height: Double) {
  require(width >= 0, "width of Dim should be positive")
  require(height >= 0, "height of Dim should be positive")

  def area = width * height
}

object Point {
  def randomPointIn(rect: Rect) = {
    val x = rect.dim.width  * Random.double + rect.point.x
    val y = rect.dim.height * Random.double + rect.point.y
    Point(x, y)
  }
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

case class Rect(point: Point, dim: Dim) {
  import scala.math.{max,min}

  def center = Point(point.x + dim.width / 2, point.y + dim.height / 2)

  val left   = point.x
  val right  = point.x + dim.width
  val top    = point.y
  val bottom = point.y + dim.height

  def area = dim.area

  def contains(p: Point) =
    left <= p.x && p.x <= right &&
    top  <= p.y && p.y <= bottom

  def <<(p: Point) = {
    val l = min(p.x, left)
    val r = max(p.x, right)
    val t = min(p.y, top)
    val b = max(p.y, bottom)
    Rect(Point(l, t), Dim(r-l, b-t))
  }

  def <<(rhs: Rect) = {
    val l = min(rhs.left,  left)
    val r = max(rhs.right, right)
    val t = min(rhs.top,   top)
    val b = max(rhs.bottom,bottom)
    Rect(Point(l, t), Dim(r-l, b-t))
  }

  def pad(p: Padding) = Rect(
    Point(point.x + p.left, point.y + p.top),
    Dim(dim.width - p.left - p.right, dim.height - p.top - p.bottom)
  )

  def isCrossingWith(other: Rect) =
    ((this.left < other.right) ^ (this.right < other.left)) &&
    ((this.top < other.bottom) ^ (this.bottom < other.top))

  def distanceWith(other: Rect) = {
    if (isCrossingWith(other)) {
      0
    } else {
      val xdiff = math.min((this.left - other.right).abs, (this.right - other.left).abs)
      val ydiff = math.min((this.top - other.bottom).abs, (this.bottom - other.top).abs)
      math.sqrt(xdiff * xdiff + ydiff * ydiff)
    }
  }
}

case class Padding(top: Double, right: Double, bottom: Double, left: Double)
