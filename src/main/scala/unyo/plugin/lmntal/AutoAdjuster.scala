package unyo.plugin.lmntal

import unyo.util._
import unyo.model._
import unyo.algorithm.{SimulatedAnnealing}

object AutoAdjuster {

  import scala.math.{log,exp}

  def run(graph: Graph) = {
    val sa = new SimulatedAnnealing[ForceParams] {
      val mi = -log(1.1)
      val ma = log(1.1)

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

      def rate(v: ForceParams) = Random.double * 100
    }

    val params = sa.run(new ForceParams)
    println(params)
  }

}
