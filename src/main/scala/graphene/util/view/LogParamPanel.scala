package graphene.util.view

import javax.swing.{JPanel}
import graphene.swing.scalalike._


/**
 * ParamControlsはJSliderとJLabelを連携させ、JSliderの変更をJLabelに反映されるように設定したコンポーネント郡で構成されます
 * sliderとlabelは自由に扱うことができ、サイズや見た目を変更したり、他のコンポーネントに追加できます
 * ただし、直接sliderやlabelの持つ値を変更した場合の動作は保証されていません
 * 値の操作の際はParamsControlsが持つ value_= メソッドなどを利用してください
 */
class ParamControls(min: Double, max: Double, iv: Double) {

  import java.awt.{Color,Dimension,BorderLayout}

  import javax.swing.{BoxLayout,JSlider,JLabel}
  import javax.swing.border.{TitledBorder}

  val slider = new JSlider(encode(min), encode(max), encode(iv)) with JSliderExt
  val label = new JLabel("%g".format(iv), javax.swing.SwingConstants.RIGHT) with JLabelExt {
    preferredSize_ = new Dimension(80, 28)
    maximumSize_ = new Dimension(80, 28)
  }

  slider.onStateChanged { _ =>
    val v = decode(slider.getValue)
    _value = v
    label.setText("%g".format(v))
    listener.map { _(v) }
  }

  protected def encode(v: Double): Int = v.toInt
  protected def decode(v: Int): Double = v.toDouble

  var _maximum = max
  def maximum = _maximum
  def maximum_=(v: Int) = {
    _maximum = v
    slider.setMaximum(encode(v))
  }

  var _minimum = min
  def minimum = _minimum
  def minimum_=(v: Int) = {
    _minimum = v
    slider.setMinimum(encode(v))
  }

  var _value = iv
  def value = _value
  def value_=(v: Int) {
    slider.setValue(encode(v))
    label.setText("%g".format(_value))
  }

  var listener = Option[Double => Unit](null)
  def onValueChanged(f: Double => Unit) {
    listener = Option(f)
  }

}

class LogParamControls(
  min: Double,
  max: Double,
  v: Double
) extends ParamControls(min, max, v) {
  protected override def encode(v: Double): Int = (math.log10(v) * 100).toInt
  protected override def decode(v: Int): Double = math.pow(10, v.toDouble / 100)
}
