package unyo.algorithm

abstract class SimulatedAnnealing[T] {

  import math.{E,abs,pow}
  import util.{Random}

  val initialTemperature = 100000.0
  val threshold = 0.1
  val rand = new Random

  def cool(t: Double): Double = t - 1.0
  def update(v: T): T
  def rate(v: T): Double
  def prob(r: Double, nr: Double, t: Double) = t / initialTemperature

  def run(value: T) = step(initialTemperature, value)

  def step(temperature: Double, v: T): T = {
    var best = v
    var bestRank = -999999.9

    var t = temperature
    var value = v
    while (t > threshold) {
      val newValue = update(value)
      val rank = rate(value)
      val newRank = rate(newValue)
      value = if (newRank > rank || prob(rank, newRank, t) > rand.nextDouble) newValue else value
      if (newRank > bestRank) {
        bestRank = newRank
        best = newValue
      }
      t = cool(t)
    }
    best
  }

}
