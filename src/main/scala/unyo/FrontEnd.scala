package unyo

import unyo.core.gui.MainFrame

object UNYO extends App {

  System.setProperty("apple.laf.useScreenMenuBar", "true");

  val frame = MainFrame.instance
  frame.setVisible(true)
  frame.pack

}
