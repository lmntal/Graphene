package unyo

import java.awt.{Toolkit,Dimension}

object Env {
  private val tk = Toolkit.getDefaultToolkit
  val frameWidth  = tk.getScreenSize.getWidth.toInt  * 2 / 3
  val frameHeight = tk.getScreenSize.getHeight.toInt * 2 / 3
}
