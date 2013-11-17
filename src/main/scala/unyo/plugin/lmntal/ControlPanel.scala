package unyo.plugin.lmntal

import javax.swing.{JPanel,JSlider,JTextField,JCheckBox,JLabel}

import unyo.swing.scalalike._

class ControlPanel(config: Config) extends JPanel with JPanelExt {

  import java.awt.{Color,Dimension,BorderLayout}
  import java.awt.event.{ActionListener,ActionEvent}
  import javax.swing.{BoxLayout}
  import javax.swing.border.{TitledBorder}
  import javax.swing.event.{ChangeListener,ChangeEvent}

  val panel = new JPanel with JPanelExt {

    import unyo.utility.view.{LogParamControls}

    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
    background_ = Color.WHITE

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("SLIM Path")
      background_ = Color.WHITE

      import javax.swing.event.{DocumentListener,DocumentEvent}
      this << new JTextField(config.slimPath) with JTextFieldExt {
        textField =>
        onTextUpdate { _ => config.slimPath = textField.getText }
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.X_AXIS)
      border_ = new TitledBorder("Repulsion")
      background_ = Color.WHITE

      val paramControls = new LogParamControls(1, 100000, config.forces.repulsion.coef1)
      paramControls.onValueChanged { config.forces.repulsion.coef1 = _ }
      this << paramControls.slider
      this << paramControls.label
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.X_AXIS)
      border_ = new TitledBorder("Spring force")
      background_ = Color.WHITE

      val paramControls = new LogParamControls(0.1, 1000, config.forces.spring.constant)
      paramControls.onValueChanged { config.forces.spring.constant = _ }
      this << paramControls.slider
      this << paramControls.label
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.X_AXIS)
      border_ = new TitledBorder("Spring length")
      background_ = Color.WHITE

      val paramControls = new LogParamControls(1, 1000, config.forces.spring.length)
      paramControls.onValueChanged { config.forces.spring.length = _ }
      this << paramControls.slider
      this << paramControls.label
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Options")
      background_ = Color.WHITE

      this << new JCheckBox("Show proxy") with JCheckBoxExt { checkBox =>
        onActionPerformed { _ => config.isProxyVisible = checkBox.isSelected }
      }
    }
  }

  layout_ = new BorderLayout
  background_ = Color.WHITE

  add(panel, BorderLayout.NORTH)

}
