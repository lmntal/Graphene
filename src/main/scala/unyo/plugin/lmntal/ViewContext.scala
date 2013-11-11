package unyo.plugin.lmntal


import unyo.utility._

class ViewContext {
  private val viewNodeFromID = collection.mutable.Map.empty[ID, View]
  val r = new util.Random

  def viewOf(node: Atom): View = viewNodeFromID.getOrElseUpdate(node.id, {
    val dim = node.id match {
      case HLAtomID(_) => Dim(10, 10)
      case _           => Dim(24, 24)
    }
    new View(Rect(Point(r.nextDouble * 800, r.nextDouble * 800), dim))
  })

  def viewOf(graph: Mem): View = viewNodeFromID.getOrElseUpdate(graph.id, {
    new View(coverableRect(graph))
  })

  def coverableRect(g: Mem): Rect = {
    val rects = g.mems.map(viewOf(_).rect) ++ g.atoms.filter(!_.isProxy).map(viewOf(_).rect)
    if (rects.isEmpty) Rect(Point(r.nextDouble * 800, r.nextDouble * 800), Dim(80, 80))
    else               rects.reduceLeft(_ << _).pad(Padding(-20, -20, -20, -20))
  }

  var rootMem: Mem = null
  def rewrite(g: Mem) {
    rootMem = g
    updateMem(rootMem)
  }
  private def updateMem(g: Mem) {
    for (node <- g.atoms) viewOf(node)
    for (graph <- g.mems) updateMem(graph)
  }

  def viewOptAt(wp: Point): Option[View] = {
    rootMem.allAtoms.map(viewOf(_)).find(_.rect.contains(wp))
  }
}

class View(var rect: Rect) {
  var speed = Point(0, 0)

  val mass = 8.0
  val decayRate = 0.90
  def force(f: Point, elapsed: Double) {
    speed = (speed + f * elapsed / mass) * decayRate
    rect = Rect(rect.point + speed * elapsed, rect.dim)
  }
}
