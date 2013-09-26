package unyo.util

object Point {
  def apply(x: Double, y: Double) = new Point(x, y)
}

object Dimension {
  def apply(w: Double, h: Double) = new Dimension(w, h)
}

class Point(val x: Double, val y: Double)
class Dimension(val width: Double, val height: Double)
