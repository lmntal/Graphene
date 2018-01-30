package graphene.core.gui

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

class LogPanelAppender extends AppenderBase[ILoggingEvent] {

  def append(event: ILoggingEvent): Unit = {
    LogPanel.println(layout.doLayout(event))
  }

  var layout: PatternLayout= _
  def getLayout(): PatternLayout = layout
  def setLayout(l: PatternLayout) = layout = l

}

import graphene.swing.scalalike._
import graphene.util._

object LogPanel extends javax.swing.JPanel with JPanelExt {

  import java.awt.{BorderLayout}

  import javax.swing.{JScrollPane,JTextPane}
  import javax.swing.text.{SimpleAttributeSet}

  val textPane = new JTextPane {
    setEditable(false)
  }

  layout_ = new BorderLayout
  this << new JScrollPane(textPane)

  def println(msg: String): Unit = {
    val doc = textPane.getDocument
    doc.insertString(doc.getLength, msg, new SimpleAttributeSet)
  }
}
