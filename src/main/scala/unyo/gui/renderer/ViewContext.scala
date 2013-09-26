package unyo.gui.renderer

import unyo.entity.{Graph,Node}
import unyo.util._

class ViewContext {
  var center = Point(0, 0)
  var size = Dimension(800, 600)
  private val viewNodeFromID = collection.mutable.Map.empty[Int, ViewNode]

  val r = new util.Random
  def viewNodeOf(node: Node): ViewNode = {
    viewNodeFromID.getOrElseUpdate(node.id, new ViewNode(r.nextDouble * 800, r.nextDouble * 800))
  }
}

class ViewNode(val x: Double, val y: Double)
