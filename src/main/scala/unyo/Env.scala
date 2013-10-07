package unyo

import java.awt.{Toolkit,Dimension}

class Env {
  val toolkit = Toolkit.getDefaultToolkit
  val size = toolkit.getScreenSize
  def frameWidth = size.width*2/3
  def frameHeight = size.height*2/3
}

