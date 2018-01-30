package graphene.core.gui

import java.awt.{Color,Dimension,BorderLayout}
import javax.swing.{JPanel,JCheckBox, JScrollPane, JTextPane, JLabel}
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
  def setPanel()={
    layout_ = new BorderLayout
    add(panel, BorderLayout.NORTH)
  }
  setPanel
  //layout_ = new BorderLayout
  //add(panel, BorderLayout.NORTH)


  val panel2 = new JPanel with JPanelExt {
    layout_ = new BoxLayout(this, BoxLayout.Y_AXIS)
    val textPane = new JTextPane {
      setEditable(false)
    }
    val scrollPane = new JScrollPane(textPane)
    this<<scrollPane
  }

  def setPanel2()={
    panel2.setPreferredSize(new Dimension(10, 50));
    add(panel2, BorderLayout.CENTER)
  }
  //setPanel2()
  //panel2.setPreferredSize(new Dimension(10, 50));
  //add(panel2, BorderLayout.CENTER)





}
