package unyo.gui

import java.awt.{Dimension}

import unyo.util._
import unyo.util.Geometry._
import unyo.Env

import unyo.swing.scalalike.{JFrameExt,JPanelExt,JMenuBarExt,JMenuExt,JMenuItemExt}

object MainFrame {
  def instance = new MainFrame
}

class MainFrame extends javax.swing.JFrame with JFrameExt {
  import javax.swing.{JMenuBar}

  closeOperation_ = javax.swing.JFrame.EXIT_ON_CLOSE

  val graphPanel = new GraphPanel
  this << graphPanel

  menuBar_ = new JMenuBar with JMenuBarExt {
    import java.awt.event.{ActionListener,ActionEvent}
    import java.awt.event.{KeyEvent,InputEvent}
    import javax.swing.{JMenu,JMenuItem,KeyStroke}

    this << new JMenu("File") with JMenuExt {
      mnemonic_ = KeyEvent.VK_F

      this << new JMenuItem("Open File") with JMenuItemExt {
        accelerator_ = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK)
        addActionListener(new ActionListener {
          override def actionPerformed(e: ActionEvent) = graphPanel.openFileChooser
        })
      }
    }
  }
}

class GraphPanel extends javax.swing.JPanel with JPanelExt {
  import scala.actors.Actor._
  import unyo.plugin.lmntal.LMNtalPlugin

  val plugin = LMNtalPlugin
  var visualGraph: plugin.GraphType = null
  val graphicsContext = new GraphicsContext
  val mover = plugin.movers(0)
  val renderer = plugin.renderers(0)
  val runtime = plugin.runtimes(0)
  val observer = plugin.observers(0)

  preferredSize_ = new Dimension(Env.frameWidth, Env.frameHeight)
  focusable_ = true

  import java.awt.event.{MouseListener,MouseMotionListener,MouseEvent}
  import java.awt.event.{MouseWheelListener,MouseWheelEvent}
  import java.awt.event.{KeyListener,KeyEvent}
  import java.awt.event.{ComponentListener,ComponentEvent}

  var prevPoint: java.awt.Point = null
  addMouseListener(new MouseListener {
    override def mouseClicked(e: MouseEvent) = {}
    override def mouseEntered(e: MouseEvent) = {}
    override def mouseExited(e: MouseEvent) = {}
    override def mousePressed(e: MouseEvent) = if (observer.canMoveScreen) prevPoint = e.getPoint
    override def mouseReleased(e: MouseEvent) = if (observer.canMoveScreen) prevPoint = null
  })

  addMouseMotionListener(new MouseMotionListener {
    override def mouseDragged(e: MouseEvent) = if (observer.canMoveScreen && prevPoint != null) {
      graphicsContext.moveBy(prevPoint - e.getPoint)
      prevPoint = e.getPoint
    }
    override def mouseMoved(e: MouseEvent) = {}
  })

  addMouseWheelListener(new MouseWheelListener {
    override def mouseWheelMoved(e: MouseWheelEvent) = graphicsContext.magnificationRate *= math.pow(1.01, e.getWheelRotation)
  })

  addKeyListener(new KeyListener {
    override def keyPressed(e: KeyEvent) = if (e.getKeyCode == KeyEvent.VK_SPACE && runtime.hasNext) visualGraph = runtime.next
    override def keyReleased(e: KeyEvent) = {}
    override def keyTyped(e: KeyEvent) = {}
  })

  addComponentListener(new ComponentListener {
    override def componentHidden(e: ComponentEvent) = {}
    override def componentMoved(e: ComponentEvent) = {}
    override def componentResized(e: ComponentEvent) = graphicsContext.resize(getSize)
    override def componentShown(e: ComponentEvent) = {}
  })

  actor {
    var prevMsec = System.currentTimeMillis
    loop {
      val msec = System.currentTimeMillis

      if (visualGraph != null) mover.moveAll(visualGraph, 1.0 * (msec - prevMsec) / 100)
      repaint()

      prevMsec = msec
      Thread.sleep(10)
    }
  }

  override def paintComponent(gg: java.awt.Graphics) {
    import java.awt.RenderingHints._

    val g = gg.asInstanceOf[java.awt.Graphics2D]

    super.paintComponent(g)

    g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    renderer.renderAll(g, graphicsContext, visualGraph)
  }

  import unyo.plugin.lmntal.LMNtalRuntime

  def openFileChooser {
    import javax.swing.{JFileChooser}

    val chooser = new JFileChooser(new java.io.File("~/")) {
      val fileFilter = new javax.swing.filechooser.FileNameExtensionFilter("LMNtal file (*.lmn)", "lmn");
      setFileFilter(fileFilter)
    }
    val res = chooser.showOpenDialog(this)
    if (res == JFileChooser.APPROVE_OPTION) {
      val file = chooser.getSelectedFile
      visualGraph = runtime.exec(Seq(file.getAbsolutePath))
      repaint()
    }
  }
}
