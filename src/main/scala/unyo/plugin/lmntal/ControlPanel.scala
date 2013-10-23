package unyo.plugin.lmntal

import javax.swing.{JPanel,JSlider}

import unyo.swing.scalalike._

class ControlPanel(forces: Forces) extends JPanel with JPanelExt {

  import java.awt.{Dimension}
  import java.awt.{BorderLayout}
  import javax.swing.{BoxLayout}
  import javax.swing.border.{TitledBorder}
  import javax.swing.event.{ChangeListener,ChangeEvent}

  val panel = new JPanel with JPanelExt {
    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)

    this << new JPanel with JPanelExt {
      layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
      border_ = new TitledBorder("Replusion")
      this << new JSlider(100000, 10000000, 1000000) {
        slider =>
        addChangeListener(new ChangeListener {
          override def stateChanged(e: ChangeEvent) {
            forces.replusion.forceBetweenAtoms = slider.getValue
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
            forces.spring.force = slider.getValue.toDouble / 10
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
            forces.spring.length = slider.getValue
          }
        })
      }
    }
  }

  layout_ = new BorderLayout
  add(panel, BorderLayout.NORTH)

}
