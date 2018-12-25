package com.evernym.agent.common.a2a

import com.evernym.agent.common.util.TransformationUtilBase
import spray.json._

case class NativeToJsonApplyParam(data: Any) extends ApplyParam
case class NativeToJsonApplyResult(result: String) extends ApplyResult

case class NativeToJsonUnapplyParam(data: String) extends UnapplyParam
case class NativeToJsonUnapplyResult(result: Any) extends UnapplyResult


class NativeToJsonTransformer
  extends Transformer[
    NativeToJsonApplyParam, NativeToJsonApplyResult,
    NativeToJsonUnapplyParam, NativeToJsonUnapplyResult] with TransformationUtilBase {

  override def apply[T, P](param: NativeToJsonApplyParam)(implicit pf: Perhaps[P]=null): NativeToJsonApplyResult = {
    implicit val rjf: RootJsonFormat[T] = pf.fold[RootJsonFormat[T]] {
      throw new RuntimeException("required implicit value missing")
    } { implicit ev =>
      ev.asInstanceOf[RootJsonFormat[T]]
    }
    val dataT = param.data.asInstanceOf[T]
    NativeToJsonApplyResult(dataT.toJson.toString)
  }

  override def unapply[T, P](param: NativeToJsonUnapplyParam)(implicit pf: Perhaps[P]=null): NativeToJsonUnapplyResult = {
    implicit val rjf: RootJsonFormat[T] = pf.fold[RootJsonFormat[T]] {
      throw new RuntimeException("required implicit value missing")
    } { implicit ev =>
      ev.asInstanceOf[RootJsonFormat[T]]
    }
    NativeToJsonUnapplyResult(param.data.parseJson.convertTo[T])
  }
}

