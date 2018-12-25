package com.evernym.agent.common.a2a


trait ApplyParam

trait ApplyResult

trait UnapplyParam

trait UnapplyResult


case class Perhaps[E](value: E) {
  def fold[F](ifAbsent: => F)(ifPresent: E => F): F = {
    Option(value).fold(ifAbsent)(ifPresent)
  }
}

trait Transformer[AP <: ApplyParam, AR <: ApplyResult, UP <: UnapplyParam, UR <: UnapplyResult] {

  implicit def perhaps[E](implicit ev: E = null): Perhaps[E] = {
    Perhaps(ev)
  }

  def apply[T, P](param: AP)(implicit pf: Perhaps[P]=null): AR

  def unapply[T, P](param: UP)(implicit pf: Perhaps[P]=null): UR

}
