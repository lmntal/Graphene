package graphene.plugin.lmntal

import graphene.model.Hot
import javax.swing.{JPanel,JTextField,JCheckBox,JButton,JToggleButton,JLabel,JList,JScrollPane,JComboBox,JColorChooser,JSlider,DefaultListModel,ListSelectionModel}
import graphene.swing.scalalike._

//画面右側のメニュー画面の中身
class ControlPanel(config: Config) extends JPanel with JPanelExt {

  import java.awt.{BorderLayout}
  import javax.swing.{BoxLayout}
  import javax.swing.border.{TitledBorder}

  val panel = new JPanel with JPanelExt {

    import graphene.util.view.{ParamControls, LogParamControls}

    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("LMNtal HOME")

      this << new JTextField(config.lmntalHome) with JTextFieldExt {
        textField =>
        onTextUpdate { _ => config.lmntalHome = textField.getText }
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("SLIM Path")

      this << new JTextField(config.slimPath) with JTextFieldExt {
        textField =>
        onTextUpdate { _ => config.slimPath = textField.getText }
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Additional options")

      this << new JTextField(config.additionalOptions) with JTextFieldExt {
        textField =>
        onTextUpdate { _ => config.additionalOptions = textField.getText }
      }
    }

    class ParamPanel(title: String, axis: Int) extends JPanel with JPanelExt {
      layout_ = new BoxLayout(this, axis)
      border_ = new TitledBorder(title)
    }

    this << new ParamPanel("Repulsion", BoxLayout.Y_AXIS) {
      val param = config.forces.repulsion
      this << new ParamPanel("Coefficient 1", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(1, 100000, param.coef1)
        paramControls.onValueChanged {
          param.coef1 = _
        }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Coefficient 2", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(1, 100000, param.coef2)
        paramControls.onValueChanged {
          param.coef2 = _
        }
        this << paramControls.slider
        this << paramControls.label
      }
    }

    this << new ParamPanel("Spring", BoxLayout.Y_AXIS) {
      val param = config.forces.spring
      this << new ParamPanel("Force", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(0.1, 1000, param.constant)
        paramControls.onValueChanged {
          param.constant = _
        }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Length", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(10, 1000, param.length)
        paramControls.onValueChanged {
          param.length = _
        }
        this << paramControls.slider
        this << paramControls.label
      }
    }

    this << new ParamPanel("Contraction", BoxLayout.Y_AXIS) {
      val param = config.forces.contraction
      this << new ParamPanel("Coefficient", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(0.01, 100, param.coef)
        paramControls.onValueChanged {
          param.coef = _
        }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Threshold of Surplus Area", BoxLayout.X_AXIS) {
        val paramControls = new ParamControls(0, 100000, param.threshold)
        paramControls.onValueChanged {
          param.threshold = _
        }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Ideal Area per Node", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(1000, 100000, param.areaPerNode)
        paramControls.onValueChanged {
          param.areaPerNode = _
        }
        this << paramControls.slider
        this << paramControls.label
      }
    }


    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Tree Layout")

      this << new ParamPanel("Vertical Gap", BoxLayout.X_AXIS) {
        val param = config.tree
        val paramControls = new LogParamControls(10, 500, param.verticalGap)
        paramControls.onValueChanged {
          param.verticalGap = _
        }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Vertical Strength", BoxLayout.X_AXIS) {
        val param = config.tree
        val paramControls = new LogParamControls(0.01, 10, param.verticalStrength)
        paramControls.onValueChanged {
          param.verticalStrength = _
        }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new JButton("Arrange Children by Link Order") with JButtonExt {
        button =>
        onActionPerformed { _ =>
          val graph = LMNtal.source.current
          if (graph != null) {
            TreeLayout.arrangeChildren(graph)
          }
        }
      }

    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Options")

      this << new JCheckBox("Show proxy") with JCheckBoxExt {
        checkBox =>
        onActionPerformed { _ => config.isProxyVisible = checkBox.isSelected }
      }
      this << new JCheckBox("Show diff") with JCheckBoxExt {
        checkBox =>
        onActionPerformed { _ => config.isDiffAnimationEnabled = checkBox.isSelected }
      }
      this << new JCheckBox("Auto focus") with JCheckBoxExt {
        checkBox =>
        onActionPerformed { _ => config.isAutoFocusEnabled = checkBox.isSelected }
      }
      
      var autoLayoutButton: JToggleButton = null
      var treeLayoutButton: JToggleButton = null
      
      autoLayoutButton = new JToggleButton(
        if (LMNtal.config.isAutoLayoutEnabled) "Auto Layout: ON" else "Auto Layout: OFF", 
        LMNtal.config.isAutoLayoutEnabled
      ) with JToggleButtonExt {
        toggleButton =>
        onActionPerformed { _ =>
          val isEnabled = toggleButton.isSelected
          config.isAutoLayoutEnabled = isEnabled
          if (isEnabled) {
            toggleButton.setText("Auto Layout: ON")
            // オートレイアウトを有効にしたときはツリーレイアウトを無効化
            config.isTreeLayoutEnabled = false
            if (treeLayoutButton != null) {
              treeLayoutButton.setSelected(false)
              treeLayoutButton.setText("Tree Layout: OFF")
            }
          } else {
            toggleButton.setText("Auto Layout: OFF")
          }
        }
      }
      this << autoLayoutButton
      
      treeLayoutButton = new JToggleButton(
        if (LMNtal.config.isTreeLayoutEnabled) "Tree Layout: ON" else "Tree Layout: OFF", 
        LMNtal.config.isTreeLayoutEnabled
      ) with JToggleButtonExt {
        toggleButton =>
        onActionPerformed { _ =>
          val isEnabled = toggleButton.isSelected
          config.isTreeLayoutEnabled = isEnabled
          if (isEnabled) {
            toggleButton.setText("Tree Layout: ON")
            // ツリーレイアウトを有効にしたときはオートレイアウトを無効化
            config.isAutoLayoutEnabled = false
            if (autoLayoutButton != null) {
              autoLayoutButton.setSelected(false)
              autoLayoutButton.setText("Auto Layout: OFF")
            }
          } else {
            toggleButton.setText("Tree Layout: OFF")
          }
        }
      }
      this << treeLayoutButton
      this << new JButton("HeatUp") with JButtonExt {
        button =>
        onActionPerformed { _ => Hot.Temperature = 250.0 }
      }
      //*
      this << new JCheckBox("Always HeatUp") with JCheckBoxExt {
        checkBox =>
        onActionPerformed { _ => Hot.Always = checkBox.isSelected }
      }
      //*/
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Atom Style")

      import java.awt.{Color, Dimension}
      import javax.swing.event.{ListSelectionListener, ListSelectionEvent}

      val styleListModel = new DefaultListModel[String]
      val styleList = new JList[String](styleListModel)
      styleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      val listScroll = new JScrollPane(styleList)
      listScroll.setPreferredSize(new Dimension(200, 120))

      val nameField = new JTextField("")
      val shapeCombo = new JComboBox[String](AtomShape.values.map(_.label).toArray)

      val sizeLabel = new JLabel("Size: 24")
      val sizeSlider = new JSlider(10, 80, 24) with JSliderExt
      sizeSlider.onStateChanged { _ => sizeLabel.setText("Size: " + sizeSlider.getValue) }

      var fillColor: Color = Color.WHITE
      var strokeColor: Color = Color.BLACK

      def colorHex(c: Color): String = f"#${c.getRed}%02X${c.getGreen}%02X${c.getBlue}%02X"

      def updateColorButton(button: JButton, color: Color): Unit = {
        button.setBackground(color)
        button.setForeground(if (color.getRed + color.getGreen + color.getBlue > 400) Color.BLACK else Color.WHITE)
        button.setOpaque(true)
        button.setBorderPainted(false)
        button.setText(colorHex(color))
      }

      def chooseColor(current: Color): Color = {
        val selected = JColorChooser.showDialog(this, "Select Color", current)
        if (selected != null) selected else current
      }

      val fillButton = new JButton("") with JButtonExt
      updateColorButton(fillButton, fillColor)
      fillButton.onActionPerformed { _ =>
        fillColor = chooseColor(fillColor)
        updateColorButton(fillButton, fillColor)
      }

      val strokeButton = new JButton("") with JButtonExt
      updateColorButton(strokeButton, strokeColor)
      strokeButton.onActionPerformed { _ =>
        strokeColor = chooseColor(strokeColor)
        updateColorButton(strokeButton, strokeColor)
      }

      def refreshList(): Unit = {
        styleListModel.clear()
        for ((name, style) <- AtomStyleRegistry.all) {
          styleListModel.addElement(s"${name} : ${style.shape.label}, size=${style.size}, fill=${colorHex(style.fillColor)}, stroke=${colorHex(style.strokeColor)}")
        }
      }

      def applyAndRepaint(): Unit = {
        val graph = LMNtal.source.current
        AtomStyleRegistry.applyToGraph(graph)
        graphene.core.gui.MainFrame.instance.mainPanel.repaint()
      }

      styleList.addListSelectionListener(new ListSelectionListener {
        override def valueChanged(e: ListSelectionEvent): Unit = {
          if (!e.getValueIsAdjusting) {
            val idx = styleList.getSelectedIndex
            if (idx >= 0 && idx < AtomStyleRegistry.all.size) {
              val (name, style) = AtomStyleRegistry.all(idx)
              nameField.setText(name)
              shapeCombo.setSelectedItem(style.shape.label)
              sizeSlider.setValue(style.size)
              fillColor = style.fillColor
              strokeColor = style.strokeColor
              updateColorButton(fillButton, fillColor)
              updateColorButton(strokeButton, strokeColor)
            }
          }
        }
      })

      val applyButton = new JButton("Apply / Update") with JButtonExt
      applyButton.onActionPerformed { _ =>
        val name = nameField.getText.trim
        if (name.nonEmpty) {
          val shape = AtomShape.fromLabel(shapeCombo.getSelectedItem.toString)
          val size = sizeSlider.getValue
          val corner = math.max(6, size / 3)
          val style = AtomStyle(fillColor, strokeColor, strokeColor, shape, size, corner)
          AtomStyleRegistry.upsert(name, style)
          refreshList()
          applyAndRepaint()
        }
      }

      val removeButton = new JButton("Remove") with JButtonExt
      removeButton.onActionPerformed { _ =>
        val name = nameField.getText.trim
        if (name.nonEmpty) {
          AtomStyleRegistry.remove(name)
          refreshList()
          applyAndRepaint()
        }
      }

      val clearButton = new JButton("Clear All") with JButtonExt
      clearButton.onActionPerformed { _ =>
        AtomStyleRegistry.clear()
        refreshList()
        applyAndRepaint()
      }

      this << new JLabel("Atom name")
      this << nameField

      this << new JLabel("Shape")
      this << shapeCombo

      this << sizeLabel
      this << sizeSlider

      this << new JLabel("Fill color")
      this << fillButton

      this << new JLabel("Stroke color")
      this << strokeButton

      this << applyButton
      this << removeButton
      this << clearButton

      this << new JLabel("Rules")
      this << listScroll

      refreshList()
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Others")

      this << new JButton("Auto adjust parameters") with JButtonExt {
        button =>
        onActionPerformed { _ => AutoAdjuster.runAsync(LMNtal.source.current) }
      }
    }

  }

  layout_ = new BorderLayout

  add(panel, BorderLayout.NORTH)

}
