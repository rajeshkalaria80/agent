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

  implicit def getRootJsonFormat[T, P](implicit oi: ImplicitParam[P]): RootJsonFormat[T] = {
    oi.fold[RootJsonFormat[T]] {
      throw new RuntimeException("required implicit RootJsonFormat value missing")
    } { implicit ev =>
      ev.asInstanceOf[RootJsonFormat[T]]
    }
  }

  override def apply[T, P](param: NativeToJsonApplyParam)
                          (implicit oi: ImplicitParam[P]=null): NativeToJsonApplyResult = {
    val dataT = param.data.asInstanceOf[T]
    NativeToJsonApplyResult(dataT.toJson.toString)
  }

  override def unapply[T, P](param: NativeToJsonUnapplyParam)
                            (implicit oi: ImplicitParam[P]=null): NativeToJsonUnapplyResult = {
    NativeToJsonUnapplyResult(param.data.parseJson.convertTo[T])
  }
}

