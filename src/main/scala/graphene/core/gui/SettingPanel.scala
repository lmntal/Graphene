package graphene.core.gui

import java.awt.{Color,Dimension,BorderLayout}
import javax.swing.{JPanel,JCheckBox}
import javax.swing.{BoxLayout}
import graphene.swing.scalalike._
import graphene.core.{Env}

class SettingPanel extends JPanel with JPanelExt {

  val panel = new JPanel with JPanelExt {
    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)

    this << new JCheckBox("Anti-alias", Env.isAntiAliasEnabled) with JCheckBoxExt { checkBox =>
      onActionPerformed { _ => graphene.core.Env.isAntiAliasEnabled = checkBox.isSelected }
    }
    this << new JCheckBox("multi core", Env.isMultiCoreEnabled) with JCheckBoxExt { checkBox =>
      onActionPerformed { _ => graphene.core.Env.isMultiCoreEnabled = checkBox.isSelected }
    }

  }

  layout_ = new BorderLayout

  add(panel, BorderLayout.NORTH)

}
