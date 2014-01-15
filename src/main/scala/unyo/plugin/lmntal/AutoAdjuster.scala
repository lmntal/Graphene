package unyo.plugin.lmntal

import unyo.util._
import unyo.model._
import unyo.algorithm.{SimulatedAnnealing}

object AutoAdjuster {

  private def combine[T](l: List[T]): List[(T,T)] = l match {
    case x :: xs => xs.map { (x, _) } ++ combine(xs)
    case Nil     => Nil
  }

  def rateGraph(graph: Graph): Double = {
    var rate = 0.0
    rate += graph.allNodes.size * 100
    rate -= overlappingArea(graph.rootNode) * 200
    rate -= crossingLinkCount(graph) * 1000
    rate += rateEdgeLength(graph)
    rate
  }

  def overlappingArea(node: Node): Double = {
    combine(node.childNodes.toList).foldLeft(0.0) { case (res, (n1,n2)) =>
      res + (n1.view.rect crossingAreaWith n2.view.rect)
    } + node.childNodes.foldLeft(0.0) { _ + overlappingArea(_) }
  }

  def crossingLinkCount(graph: Graph) = {
    combine(graph.allEdges.toList.map { e =>
      Line.from(e.source.view.rect.center).to(e.target.view.rect.center)
    }).count { case (line1, line2) =>
      line1 isCrossing line2
    }
  }

  def allEdgesLength(graph: Graph) = {
    graph.allEdges.map { e =>
      Line.from(e.source.view.rect.center).to(e.target.view.rect.center).length
    }.foldLeft(0.0) { _ + _ }
  }

  def rateEdgeLength(graph: Graph) = {
    graph.allEdges.map { e =>
      val len = Line.from(e.source.view.rect.center).to(e.target.view.rect.center).length
      if      (len > 120) 80 - len
      else if (len <  50) (len - 50) * 10
      else                0
    }.foldLeft(0.0) { _ + _ }
  }

  import scala.math.{log,exp}
  import scala.actors.Actor.actor

  def runAsync(graph: Graph) = actor { run(graph) }

  def run(graph: Graph) = {
    val sa = new SimulatedAnnealing[ForceParams] {
      val mi = -log(1.1)
      val ma = log(1.1)

      override def cool(t: Double): Double = t - 10

      def update(v: ForceParams) = {
        val params = new ForceParams

        params.repulsion.coef1         = exp(mi + Random.double * (ma - mi)) * v.repulsion.coef1
        params.repulsion.coef2         = exp(mi + Random.double * (ma - mi)) * v.repulsion.coef2
        params.spring.constant         = exp(mi + Random.double * (ma - mi)) * v.spring.constant
        params.spring.length           = exp(mi + Random.double * (ma - mi)) * v.spring.length
        params.contraction.coef        = exp(mi + Random.double * (ma - mi)) * v.contraction.coef
        params.contraction.threshold   = exp(mi + Random.double * (ma - mi)) * v.contraction.threshold
        params.contraction.areaPerNode = exp(mi + Random.double * (ma - mi)) * v.contraction.areaPerNode

        params
      }

      def rate(params: ForceParams) = {
        val g = graph.deepcopy
        for (i <- 0 to 90) FastMover.moveAll(g, 1.0 / 30, params)
        rateGraph(g)
      }
    }

    val params = sa.run(new ForceParams)
    println(params)
  }

}
