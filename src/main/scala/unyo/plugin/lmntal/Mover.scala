package unyo.plugin.lmntal

import unyo.util._

class DefaultMover extends LMNtalPlugin.Mover {

  var viewContext: ViewContext = null
  def moveAll(viewContext: ViewContext, elapsedSec: Double) {
    if (viewContext == null || viewContext.rootMem == null) return
    this.viewContext = viewContext
    move(viewContext.rootMem, elapsedSec)
  }

  def move(graph: Mem, elapsedSec: Double) {
    val allAtoms = allAtomsIn(graph)

    for (subgraph <- graph.mems) move(subgraph, elapsedSec)

    for (node <- graph.atoms) {
      val v1 = viewContext.viewOf(node)
      var vec = Point(0, 0)

      vec = vec + forceOfReplusion(node, allAtoms)
      vec = vec + forceOfSpring(node)

      v1.force(vec, elapsedSec)
    }

    resizeGraphArea(graph)
  }

  private def resizeGraphArea(graph: Mem) {
    graph.mems.foreach(resizeGraphArea(_))
    val view = viewContext.viewOf(graph)
    view.rect = viewContext.coverableRect(graph)
  }

  private def forceOfReplusion(self: Atom, allAtoms: collection.Set[Atom]): Point = {
    var vec = Point(0, 0)
    val v1 = viewContext.viewOf(self)
    for (other <- allAtoms) {
      if (self != other) {
        val v2 = viewContext.viewOf(other)
        val d = v2.rect.center - v1.rect.center
        val f = 1000000.0 / d.sqabs
        vec = vec - d.unit * f
      }
    }
    vec
  }

  private def forceOfSpring(self: Atom): Point = {
    var vec = Point(0, 0)
    val v1 = viewContext.viewOf(self)
    for (i <- 0 until self.arity) {
      val other = self.buddyAt(i)
      val v2 = viewContext.viewOf(other)
      val d = v2.rect.center - v1.rect.center
      val f = 2.0 * (d.abs - 120)
      vec = vec + d.unit * f
    }
    vec
  }

  private def allAtomsIn(graph: Mem): collection.Set[Atom] = {
    val lhs: collection.Set[Atom] = graph.atoms
    val rhs: collection.Set[Atom] = graph.mems.map(allAtomsIn _).flatten
    lhs ++ rhs
  }

}
