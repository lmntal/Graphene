package unyo.entity

object Graphy {
  def fromString(s: String): Graph = lmntal.Membrane.fromString(s)
}

trait Graph {
  def id: Int
  def name: String
  def nodes: collection.Set[_ <: Node]
  def graphs: collection.Set[_ <: Graph]
}

trait Node {
  def id: Int
  def name: String
  def arity: Int
  def buddyAt(i: Int): Node
}

trait Edge {
  def node: Node
}

