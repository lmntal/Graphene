package unyo.plugin.lmntal

import javax.swing.{JPanel,JSlider,JTextField,JCheckBox}

import unyo.swing.scalalike._

class ControlPanel(config: Config) extends JPanel with JPanelExt {

  import java.awt.{Dimension}
  import java.awt.{BorderLayout}
  import java.awt.event.{ActionListener,ActionEvent}
  import javax.swing.{BoxLayout}
  import javax.swing.border.{TitledBorder}
  import javax.swing.event.{ChangeListener,ChangeEvent}

  val panel = new JPanel with JPanelExt {
    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("SLIM Path")

      import javax.swing.event.{DocumentListener,DocumentEvent}
      this << new JTextField(config.slimPath) {
        textField =>
        getDocument.addDocumentListener(new DocumentListener {
          override def changedUpdate(e: DocumentEvent) = {}
          override def insertUpdate(e: DocumentEvent) = config.slimPath = textField.getText
          override def removeUpdate(e: DocumentEvent) = config.slimPath = textField.getText
        })
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Repulsion")
      this << new JSlider(100000, 10000000, 1000000) {
        slider =>
        addChangeListener(new ChangeListener {
          override def stateChanged(e: ChangeEvent) {
            config.forces.repulsion.coef1 = slider.getValue
          }
        })
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Spring force")
      this << new JSlider(1, 1000, 20) {
        slider =>
        addChangeListener(new ChangeListener {
          override def stateChanged(e: ChangeEvent) {
            config.forces.spring.constant = slider.getValue.toDouble / 10
          }
        })
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Spring length")
      this << new JSlider(1, 1000, 120) {
        slider =>
        addChangeListener(new ChangeListener {
          override def stateChanged(e: ChangeEvent) {
            config.forces.spring.length = slider.getValue
          }
        })
      }
    }

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Options")
      this << new JCheckBox("Show proxy") {
        checkBox =>
        addActionListener(new ActionListener {
          override def actionPerformed(e: ActionEvent) {
            config.isProxyVisible = checkBox.isSelected
          }
        })
      }
    }
  }

  layout_ = new BorderLayout
  add(panel, BorderLayout.NORTH)

}
