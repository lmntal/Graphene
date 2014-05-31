package graphene.swing.scalalike

/*
scala.swingは使いにくい
でもswingを宣言的な書き方にしたい
そんな時に役に立つかもしれないExtです
*/

import java.awt.{Point,Component}


trait Event
case class MouseClicked(source: Component, point: Point, modifiers: Int, clicks: Int, triggersPopup: Boolean) extends Event
case class MouseEntered(source: Component, point: Point, modifiers: Int) extends Event
case class MouseExited(source: Component, point: Point, modifiers: Int) extends Event
case class MousePressed(source: Component, point: Point, modifiers: Int, clicks: Int, triggersPopup: Boolean) extends Event
case class MouseReleased(source: Component, point: Point, modifiers: Int, clicks: Int, triggersPopup: Boolean) extends Event
case class MouseMoved(source: Component, point: Point, modifiers: Int) extends Event
case class MouseDragged(source: Component, point: Point, modifiers: Int) extends Event
case class MouseWheelMoved(source: Component, point: Point, modifiers: Int, rotation: Int) extends Event
case class KeyPressed(source: Component, key: Int, modifiers: Int, location: Int) extends Event
case class KeyReleased(source: Component, key: Int, modifiers: Int, location: Int) extends Event
case class KeyTyped(source: Component, char: Char, modifiers: Int, location: Int) extends Event
case class ComponentHidden(source: Component) extends Event
case class ComponentMoved(source: Component) extends Event
case class ComponentResized(source: Component) extends Event
case class ComponentShown(source: Component) extends Event


object Reactions {
  type Reaction = PartialFunction[Event,Unit]
}

trait ComponentExt {
  self: java.awt.Component =>

  import collection.mutable.Buffer

  val reactions = Buffer.empty[Reactions.Reaction]

  private def dispatch(e: Event) = for (r <- reactions) if (r.isDefinedAt(e)) r(e)

  import java.awt.event.{ComponentListener,ComponentEvent}

  def listenToComponent() = addComponentListener(new ComponentListener {
    override def componentHidden(e: ComponentEvent) =
      dispatch(ComponentHidden(e.getComponent))
    override def componentMoved(e: ComponentEvent) =
      dispatch(ComponentMoved(e.getComponent))
    override def componentResized(e: ComponentEvent) =
      dispatch(ComponentResized(e.getComponent))
    override def componentShown(e: ComponentEvent) =
      dispatch(ComponentShown(e.getComponent))
  })

  import java.awt.event.{MouseListener,MouseEvent}

  def listenToMouse() = addMouseListener(new MouseListener {
    override def mouseClicked(e: MouseEvent) =
      dispatch(MouseClicked(e.getComponent, e.getPoint, e.getModifiers, e.getClickCount, e.isPopupTrigger))
    override def mouseEntered(e: MouseEvent) =
      dispatch(MouseEntered(e.getComponent, e.getPoint, e.getModifiers))
    override def mouseExited(e: MouseEvent) =
      dispatch(MouseExited(e.getComponent, e.getPoint, e.getModifiers))
    override def mousePressed(e: MouseEvent) =
      dispatch(MousePressed(e.getComponent, e.getPoint, e.getModifiers, e.getClickCount, e.isPopupTrigger))
    override def mouseReleased(e: MouseEvent) =
      dispatch(MouseReleased(e.getComponent, e.getPoint, e.getModifiers, e.getClickCount, e.isPopupTrigger))
  })

  import java.awt.event.{MouseMotionListener}

  def listenToMouseMotion() = addMouseMotionListener(new MouseMotionListener {
    override def mouseMoved(e: MouseEvent) =
      dispatch(MouseMoved(e.getComponent, e.getPoint, e.getModifiers))
    override def mouseDragged(e: MouseEvent) =
      dispatch(MouseDragged(e.getComponent, e.getPoint, e.getModifiers))
  })

  import java.awt.event.{MouseWheelListener,MouseWheelEvent}

  def listenToMouseWheel() = addMouseWheelListener(new MouseWheelListener {
    override def mouseWheelMoved(e: MouseWheelEvent) =
      dispatch(MouseWheelMoved(e.getComponent, e.getPoint, e.getModifiers, e.getWheelRotation))
  })

  import java.awt.event.{KeyListener,KeyEvent}

  def listenToKey() = addKeyListener(new KeyListener {
    override def keyPressed(e: KeyEvent) =
      dispatch(KeyPressed(e.getComponent, e.getKeyCode, e.getModifiers, e.getKeyLocation))
    override def keyReleased(e: KeyEvent) =
      dispatch(KeyReleased(e.getComponent, e.getKeyCode, e.getModifiers, e.getKeyLocation))
    override def keyTyped(e: KeyEvent) =
      dispatch(KeyTyped(e.getComponent, e.getKeyChar, e.getModifiers, e.getKeyLocation))
  })

  def bounds_ = getBounds
  def bounds__=(v: java.awt.Rectangle) = setBounds(v)

  def componentOrientation_ = getComponentOrientation
  def componentOrientation__= = setComponentOrientation _

  def cursor_ = getCursor
  def cursor__= = setCursor _

  def dropTarget_ = getDropTarget
  def dropTarget__= = setDropTarget _

  def focusTraversalKeysEnabled_ = getFocusTraversalKeysEnabled
  def focusTraversalKeysEnabled__= = setFocusTraversalKeysEnabled _

  def focusable_ = isFocusable
  def focusable__= = setFocusable _

  def ignoreRepaint_ = getIgnoreRepaint
  def ignoreRepaint__= = setIgnoreRepaint _

  def locale_ = getLocale
  def locale__= = setLocale _

  def location_ = getLocation
  def location__=(v: java.awt.Point) = setLocation(v)

  def name_ = getName
  def name__= = setName _

  def size_ = getSize
  def size__=(v: java.awt.Dimension) = setSize(v)
}

trait ContainerExt extends ComponentExt {
  self: java.awt.Container =>

  def <<(c: Component) = add(c)

  def layout_ = getLayout
  def layout__= = setLayout _
}

trait JComponentExt extends ContainerExt {
  self: javax.swing.JComponent =>

  def background_ = getBackground
  def background__= = setBackground _

  def border_ = getBorder
  def border__= = setBorder _

  def doubleBuffered_ = isDoubleBuffered
  def doubleBuffered__= = setDoubleBuffered _

  def font_ = getFont
  def font__= = setFont _

  def foreground_ = getForeground
  def foreground__= = setForeground _

  def maximumSize_ = getMaximumSize
  def maximumSize__= = setMaximumSize _

  def minimumSize_ = getMinimumSize
  def minimumSize__= = setMinimumSize _

  def opaque_ = isOpaque
  def opaque__= = setOpaque _

  def preferredSize_ = getPreferredSize
  def preferredSize__= = setPreferredSize _

  def requestFocusEnabled_ = isRequestFocusEnabled
  def requestFocusEnabled__= = setRequestFocusEnabled _

  def toolTipText_ = getToolTipText
  def toolTipText__= = setToolTipText _

  def visible_ = isVisible
  def visible__= = setVisible _
}

trait JSliderExt extends JComponentExt {
  self: javax.swing.JSlider =>

  import javax.swing.event.{ChangeListener,ChangeEvent}

  def onStateChanged(f: ChangeEvent => Unit) {
    addChangeListener(new ChangeListener {
      override def stateChanged(e: ChangeEvent) { f(e) }
    })
  }

}

trait JSplitPaneExt extends JComponentExt {
  self: javax.swing.JSplitPane =>

  def leftComponent_ = getLeftComponent
  def leftComponent__= = setLeftComponent _

  def rightComponent_ = getRightComponent
  def rightComponent__= = setRightComponent _
}

trait AbstractButtonExt extends JComponentExt {
  self: javax.swing.AbstractButton =>

  import java.awt.event.{ActionListener,ActionEvent}

  def onActionPerformed(f: ActionEvent => Unit) {
    addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent) { f(e) }
    })
  }

  def action_ = getAction
  def action__= = setAction _

  def actionCommand_ = getActionCommand
  def actionCommand__= = setActionCommand _

  def borderPainted_ = isBorderPainted
  def borderPainted__= = setBorderPainted _

  def contentAreaFilled_ = isContentAreaFilled
  def contentAreaFilled__= = setContentAreaFilled _

  def disabledIcon_ = getDisabledIcon
  def disabledIcon__= = setDisabledIcon _

  def disabledSelectedIcon_ = getDisabledSelectedIcon
  def disabledSelectedIcon__= = setDisabledSelectedIcon _

  def displayedMnemonicIndex_ = getDisplayedMnemonicIndex
  def displayedMnemonicIndex__= = setDisplayedMnemonicIndex _

  def focusPainted_ = isFocusPainted
  def focusPainted__= = setFocusPainted _

  def hideActionText_ = getHideActionText
  def hideActionText__= = setHideActionText _

  def horizontalAlignment_ = getHorizontalAlignment
  def horizontalAlignment__= = setHorizontalAlignment _

  def horizontalTextPosition_ = getHorizontalTextPosition
  def horizontalTextPosition__= = setHorizontalTextPosition _

  def icon_ = getIcon
  def icon__= = setIcon _

  def iconTextGap_ = getIconTextGap
  def iconTextGap__= = setIconTextGap _

  def margin_ = getMargin
  def margin__= = setMargin _

  def mnemonic_ = getMnemonic
  def mnemonic__= = setMnemonic _

  def multiClickThreshhold_ = getMultiClickThreshhold
  def multiClickThreshhold__= = setMultiClickThreshhold _

  def pressedIcon_ = getPressedIcon
  def pressedIcon__= = setPressedIcon _

  def rolloverEnabled_ = isRolloverEnabled
  def rolloverEnabled__= = setRolloverEnabled _

  def rolloverIcon_ = getRolloverIcon
  def rolloverIcon__= = setRolloverIcon _

  def rolloverSelectedIcon_ = getRolloverSelectedIcon
  def rolloverSelectedIcon__= = setRolloverSelectedIcon _

  def selectedIcon_ = getSelectedIcon
  def selectedIcon__= = setSelectedIcon _

  def text_ = getText
  def text__= = setText _

  def verticalAlignment_ = getVerticalAlignment
  def verticalAlignment__= = setVerticalAlignment _

  def verticalTextPosition_ = getVerticalTextPosition
  def verticalTextPosition__= = setVerticalTextPosition _
}

trait JButtonExt extends AbstractButtonExt {
  self: javax.swing.JButton =>
}

trait JPanelExt extends JComponentExt{
  self: javax.swing.JPanel =>
}

trait JFrameExt extends ContainerExt {
  self: javax.swing.JFrame =>

  import java.awt.{Component}
  import javax.swing.{JMenuBar}

  def closeOperation_ = getDefaultCloseOperation
  def closeOperation__=(v: Int) = setDefaultCloseOperation(v)

  def menuBar_ = getJMenuBar
  def menuBar__=(v: JMenuBar) = setJMenuBar(v)
}

trait JMenuBarExt {
  self: javax.swing.JMenuBar =>

  import javax.swing.{JMenu}

  def <<(c: JMenu) = add(c)
}

trait JMenuItemExt extends AbstractButtonExt {
  self: javax.swing.JMenuItem =>

  import javax.swing.{KeyStroke}

  def accelerator_ = getAccelerator
  def accelerator__=(v: KeyStroke) = setAccelerator(v)
}

trait JMenuExt extends JMenuItemExt {
  self: javax.swing.JMenu =>

  import javax.swing.{JMenuItem}

  def <<(c: JMenuItem) = add(c)
}

trait JFileChooserExt extends JComponentExt {
  self: javax.swing.JFileChooser =>

  def fileFilter_ = getFileFilter
  def fileFilter__= = setFileFilter _

  def selectedFile = getSelectedFile
}

trait JToggleButtonExt extends AbstractButtonExt {
  self: javax.swing.JToggleButton =>
}

trait JCheckBoxExt extends JToggleButtonExt {
  self: javax.swing.JCheckBox =>
}


trait JTextComponentExt extends JComponentExt {
  self: javax.swing.text.JTextComponent =>

  import javax.swing.event.{DocumentListener,DocumentEvent}

  def onTextUpdate(f: DocumentEvent => Unit) {
    getDocument.addDocumentListener(new DocumentListener {
      override def changedUpdate(e: DocumentEvent) { f(e) }
      override def insertUpdate(e: DocumentEvent) { f(e) }
      override def removeUpdate(e: DocumentEvent) { f(e) }
    })
  }
}

trait JTextFieldExt extends JTextComponentExt {
  self: javax.swing.JTextField =>
}

trait JLabelExt extends JComponentExt {
  self: javax.swing.JLabel =>
}
