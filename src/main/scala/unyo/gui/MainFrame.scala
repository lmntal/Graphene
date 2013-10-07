package unyo.gui

import scala.swing.{Frame,FlowPanel,FileChooser}
import scala.swing.{MenuBar,Menu,MenuItem,Action}
import scala.swing.event.{KeyPressed}
import scala.swing.event.Key

import java.awt.{Dimension}
import java.awt.event.{KeyEvent,InputEvent}
import javax.swing.KeyStroke
import javax.swing.filechooser.{FileFilter,FileNameExtensionFilter}

import unyo.runtime.LMNtalRuntime
import unyo.Env

object MainFrame {
  def instance = new MainFrame
}

class MainFrame extends Frame {
  override def closeOperation = dispose

  menuBar = new MenuBar {
    contents += new Menu("File") {
      mnemonic = Key.F

      val fileAction = Action("Open File") { openFileChooser }
      fileAction.accelerator = Option(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK))
      contents += new MenuItem(fileAction) { mnemonic = Key.O }
    }
  }

  val graphPanel: GraphPanel = new GraphPanel {
    preferredSize = new Dimension(Env.frameWidth, Env.frameHeight)
    focusable = true

    listenTo(this.keys)
    reactions += {
      case KeyPressed(_, key, _, _) => {
        if (key == Key.Space) {
          if (runtime.hasNext) {
            val graph = runtime.next
            graphPanel.setGraph(graph)
          }
        }
      }
    }
  }

  contents = graphPanel

  var runtime: LMNtalRuntime = null
  def openFileChooser {
    val chooser = new FileChooser(new java.io.File("~/")) {
      fileFilter = new FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
    }
    val res = chooser.showOpenDialog(this.contents(0))
    if (res == FileChooser.Result.Approve) {
      val file = chooser.selectedFile
      runtime = new LMNtalRuntime(file, java.util.Arrays.asList("-O", "--hide-rule", "--hide-ruleset"))
      val graph = runtime.next
      graphPanel.setGraph(graph)
    }
  }
}
