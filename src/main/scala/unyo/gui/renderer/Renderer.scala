package unyo.gui.renderer

import java.awt.{Graphics,Color}

class DefaultRenderer(val g: Graphics) {

  def render() {
    g.setColor(Color.BLUE)
    g.drawRect(20, 20, 100, 100)
  }
}
