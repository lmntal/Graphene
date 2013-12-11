package unyo

import unyo.core.gui.MainFrame

object FrontEnd extends App {

  val frame = MainFrame.instance
  frame.setVisible(true)
  frame.pack

}
