package graphene.algorithm

import graphene.util._
import math.{pow,sqrt}

object ForceBased {
  def repulsion(self: Rect, others: Seq[Rect], coef1: Double, coef2: Double): Point =
    others.foldLeft(Point.zero) { (res, other) => res + repulsion(self, other, coef1, coef2) }

  def repulsion(self: Rect, other: Rect, coef1: Double, coef2: Double): Point = {
    val dist = self.distanceWith(other)
    val d = self.center - other.center
    val f = coef1 / (dist * dist / coef2 + 1)
    d.unit * f
  }

  def spring(self: Point, others: Seq[Point], constant: Double, length: Double): Point =
    others.foldLeft(Point.zero) { (res, other) => res + spring(self, other, constant, length) }

  def spring(self: Point, other: Point, constant: Double, length: Double): Point = {
    val d = other - self
    val f = constant * (d.abs - length)
    d.unit * f
  }

  def attraction(self: Point, origin: Point, coef: Double, dist: Double) = {
    val d = origin - self
    val f = coef *  sqrt(d.abs * dist)
    d.unit * f
  }
}
