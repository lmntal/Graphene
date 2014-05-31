package graphene.util

object Random {
  val r = new util.Random
  def double = r.nextDouble
  def int(n: Int) = r.nextInt(n)
  def int = r.nextInt
}
