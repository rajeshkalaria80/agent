package com.evernym.agent.common.a2a


trait ApplyParam

trait ApplyResult

trait UnapplyParam

trait UnapplyResult


trait Transformer[AP <: ApplyParam, AR <: ApplyResult, UP <: UnapplyParam, UR <: UnapplyResult] {

  def apply[T](data: T, paramOpt: Option[AP]=None): AR

  def unapply[T](data: T, paramOpt: Option[UP]=None): UR

}
