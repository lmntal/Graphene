package unyo

import unyo.gui.MainFrame

object FrontEnd extends App {

  val frame = MainFrame.instance
  frame.visible = true
  frame.pack

}
