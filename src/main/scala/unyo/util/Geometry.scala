package unyo.util

object Geometry {
  import scala.language.implicitConversions

  implicit def toUnyoDimension(d: java.awt.Dimension) = Dim(d.width, d.height)
  implicit def toAWTDimension(d: Dim) = new java.awt.Dimension(d.width.toInt, d.height.toInt)

  implicit def toUnyoPoint(p: java.awt.Point) = Point(p.x, p.y)
  implicit def toAWTPoint(p: Point) = new java.awt.Point(p.x.toInt, p.y.toInt)
}

case class Dim(width: Double, height: Double) {
  require(width >= 0, s"width of ${this} should be positive")
  require(height >= 0, s"height of ${this} should be positive")

  def area = width * height
  def toPoint = Point(width, height)
}

object Point {
  val zero = Point(0, 0)

  def randomPointIn(rect: Rect) = {
    val x = rect.dim.width  * Random.double + rect.point.x
    val y = rect.dim.height * Random.double + rect.point.y
    Point(x, y)
  }
}

case class Point(x: Double, y: Double) {
  require(!java.lang.Double.isNaN(x), s"x of ${this} should not be NaN")
  require(!java.lang.Double.isNaN(y), s"y of ${this} should not be NaN")

  def +(other: Point) = Point(x + other.x, y + other.y)
  def -(other: Point) = Point(x - other.x, y - other.y)
  def *(other: Double) = Point(x * other, y * other)
  def /(other: Double) = Point(x / other, y / other)
  def dot(other: Point) = x*other.x + y*other.y
  def sqabs: Double = dot(this)
  def abs: Double = math.hypot(x, y)
  def unit: Point = if (x.abs < 1e-9 && y.abs < 1e-9) Point.zero else this / abs
  def rotate(rad: Double) = Point(x * math.cos(rad) - y * math.sin(rad), x * math.sin(rad) + y * math.cos(rad))
}

object Line {

  class LineBuilder(from: Point) {
    def to(p: Point) = Line(from, p)
  }

  def from(p: Point) = new LineBuilder(p)
}

case class Line(from: Point, to: Point) {
  def isCrossing(other: Line) = {

    def side(p: Point, l: Line) = {
      val x1 = p.x
      val y1 = p.y
      val x2 = l.from.x
      val y2 = l.from.y
      val x3 = l.to.x
      val y3 = l.to.y

      (x1 - x2) * (y3 - y1) + (y1 - y2) * (x1 - x3)
    }

    val p1 = from
    val p2 = to
    val p3 = other.from
    val p4 = other.to

    side(p1, other) * side(p2, other) < 0 && side(p3, this) * side(p4, this) < 0
  }

  def length = (to - from).abs
}

case class Rect(point: Point, dim: Dim) {
  import scala.math.{max,min,abs}

  val center = Point(point.x + dim.width / 2, point.y + dim.height / 2)

  def left   = point.x
  def right  = point.x + dim.width
  def top    = point.y
  def bottom = point.y + dim.height

  def area = dim.area

  def contains(p: Point) =
    left <= p.x && p.x <= right &&
    top  <= p.y && p.y <= bottom

  def movedBy(p: Point) = Rect(point + p, dim)

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

  def crossingAreaWith(other: Rect) = {
    val w = if ((this.left < other.right) ^ (this.right < other.left))
              min(this.right, other.right) - max(this.left, other.left)
            else
              0
    val h = if ((this.top < other.bottom) ^ (this.bottom < other.top))
              min(this.bottom, other.bottom) - max(this.top, other.top)
            else
              0
    w * h
  }

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
