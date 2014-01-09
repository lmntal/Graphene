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

    import unyo.util.view.{ParamControls,LogParamControls}

    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("LMNtal HOME")

      import javax.swing.event.{DocumentListener,DocumentEvent}
      this << new JTextField(config.lmntalHome) with JTextFieldExt {
        textField =>
        onTextUpdate { _ => config.lmntalHome = textField.getText }
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("SLIM Path")

      import javax.swing.event.{DocumentListener,DocumentEvent}
      this << new JTextField(config.slimPath) with JTextFieldExt {
        textField =>
        onTextUpdate { _ => config.slimPath = textField.getText }
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Additional options")

      import javax.swing.event.{DocumentListener,DocumentEvent}
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
        paramControls.onValueChanged { param.coef1 = _ }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Coefficient 2", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(1, 100000, param.coef2)
        paramControls.onValueChanged { param.coef2 = _ }
        this << paramControls.slider
        this << paramControls.label
      }
    }

    this << new ParamPanel("Spring", BoxLayout.Y_AXIS) {
      val param = config.forces.spring
      this << new ParamPanel("Force", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(0.1, 1000, param.constant)
        paramControls.onValueChanged { param.constant = _ }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Length", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(10, 1000, param.length)
        paramControls.onValueChanged { param.length = _ }
        this << paramControls.slider
        this << paramControls.label
      }
    }

    this << new ParamPanel("Contraction", BoxLayout.Y_AXIS) {
      val param = config.forces.contraction
      this << new ParamPanel("Coefficient", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(0.01, 100, param.coef)
        paramControls.onValueChanged { param.coef = _ }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Threshold of Surplus Area", BoxLayout.X_AXIS) {
        val paramControls = new ParamControls(0, 100000, param.threshold)
        paramControls.onValueChanged { param.threshold = _ }
        this << paramControls.slider
        this << paramControls.label
      }

      this << new ParamPanel("Ideal Area per Node", BoxLayout.X_AXIS) {
        val paramControls = new LogParamControls(1000, 100000, param.areaPerNode)
        paramControls.onValueChanged { param.areaPerNode = _ }
        this << paramControls.slider
        this << paramControls.label
      }
    }


    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Options")

      this << new JCheckBox("Show proxy") with JCheckBoxExt { checkBox =>
        onActionPerformed { _ => config.isProxyVisible = checkBox.isSelected }
      }
      this << new JCheckBox("Show diff") with JCheckBoxExt { checkBox =>
        onActionPerformed { _ => config.isDiffAnimationEnabled = checkBox.isSelected }
      }
    }
  }

  layout_ = new BorderLayout

  add(panel, BorderLayout.NORTH)

}
