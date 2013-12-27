package unyo.util

import java.awt.{Color => JColor}

import scala.collection.{Iterator}

object Color {
  def fromHSB(h: Float, s: Float, b: Float) = new JColor(JColor.HSBtoRGB(h, s, b))
}

class RandomColorGenerator(colorCount: Int, step: Int) {

  def this() = this(21, 8)

  var index = Random.int(colorCount)
  def next: JColor = {
    val f = 1.0f * (index % colorCount) / colorCount
    index += step
    Color.fromHSB(f, 0.55f, 0.85f)
  }

}

object Palette {
  val lightGray = Color.fromHSB(0.8f, 0.05f, 0.75f)
}
