package graphene.plugin.lmntal

import java.awt.Color

import graphene.model.{Graph, Node}
import graphene.util.{Dim, Point, Rect}

import scala.collection.mutable

sealed trait AtomShape {
  def label: String
}

object AtomShape {
  case object Circle extends AtomShape {
    val label = "Circle"
  }
  case object RoundedRect extends AtomShape {
    val label = "RoundedRect"
  }

  val values: Seq[AtomShape] = Seq(Circle, RoundedRect)

  def fromLabel(label: String): AtomShape = values.find(_.label == label).getOrElse(Circle)
}

case class AtomStyle(
  fillColor: Color,
  strokeColor: Color,
  textColor: Color,
  shape: AtomShape,
  size: Int,
  cornerRadius: Int
)

object AtomStyleRegistry {
  private val styles = mutable.LinkedHashMap.empty[String, AtomStyle]
  val DefaultAtomSize = 24

  def all: Seq[(String, AtomStyle)] = styles.toSeq

  def get(name: String): Option[AtomStyle] = styles.get(normalize(name))

  def upsert(name: String, style: AtomStyle): Unit = {
    val key = normalize(name)
    if (key.nonEmpty) styles.update(key, style)
  }

  def remove(name: String): Unit = styles.remove(normalize(name))

  def clear(): Unit = styles.clear()

  def styleFor(node: Node): Option[AtomStyle] =
    if (node.attr == Atom) get(node.name) else None

  def applyToGraph(graph: Graph): Unit = {
    if (graph != null) graph.allNodes.foreach(applyToNode)
  }

  def applyToNode(node: Node): Unit = {
    if (node.attr == Atom) {
      val styleOpt = styleFor(node)
      val sizeValue = styleOpt.map(_.size).getOrElse(DefaultAtomSize)
      val rect = node.view.rect
      val center = rect.center
      val size = math.max(4, sizeValue).toDouble
      node.view.rect = Rect(Point(center.x - size / 2.0, center.y - size / 2.0), Dim(size, size))
    }
  }

  private def normalize(name: String): String = name.trim
}
