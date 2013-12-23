package unyo.core.gui

import java.awt.{Color,Dimension,BorderLayout}
import javax.swing.{JPanel,JCheckBox}
import javax.swing.{BoxLayout}
import unyo.swing.scalalike._
import unyo.core.{Env}

class SettingPanel extends JPanel with JPanelExt {

  val panel = new JPanel with JPanelExt {
    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
    background_ = Color.WHITE

    this << new JCheckBox("Anti-alias", Env.isAntiAliasEnabled) with JCheckBoxExt { checkBox =>
      onActionPerformed { _ => unyo.core.Env.isAntiAliasEnabled = checkBox.isSelected }
    }
    this << new JCheckBox("multi core", Env.isMultiCoreEnabled) with JCheckBoxExt { checkBox =>
      onActionPerformed { _ => unyo.core.Env.isMultiCoreEnabled = checkBox.isSelected }
    }

  }

  layout_ = new BorderLayout
  background_ = Color.WHITE

  add(panel, BorderLayout.NORTH)

}
