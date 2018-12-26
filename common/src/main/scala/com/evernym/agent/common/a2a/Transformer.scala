package com.evernym.agent.common.a2a


trait ApplyParam

trait ApplyResult

trait UnapplyParam

trait UnapplyResult


case class ImplicitParam[E](value: E) {
  def fold[F](ifAbsent: => F)(ifPresent: E => F): F = {
    Option(value).fold(ifAbsent)(ifPresent)
  }
}

trait Transformer[AP <: ApplyParam, AR <: ApplyResult, UP <: UnapplyParam, UR <: UnapplyResult] {

  def apply[T, P](param: AP)(implicit oi:  ImplicitParam[P]=null): AR

  def unapply[T, P](param: UP)(implicit oi:  ImplicitParam[P]=null): UR

}
