package com.evernym.agent.common.a2a

import com.evernym.agent.common.util.TransformationUtilBase
import spray.json.RootJsonFormat


trait NativeToJsonParam

case class NativeToJsonApplyParam[T](data: T)(implicit val rjf: RootJsonFormat[T]) extends NativeToJsonParam

case class NativeToJsonApplyResult(msg: String)
case class NativeToJsonUnapplyResult(msg: Any)


class NativeToJsonTransformer
  extends Transformer[
    Any, NativeToJsonApplyResult,
    Any, NativeToJsonUnapplyResult] with TransformationUtilBase {

  override def apply[T](data: T, paramOpt: Option[Any]=None): NativeToJsonApplyResult = {
    val inputData = data.asInstanceOf[NativeToJsonApplyParam[T]]
    NativeToJsonApplyResult(convertNativeMsgToJson(inputData.data)(inputData.rjf))
  }

  override def unapply[T](data: T, paramOpt: Option[Any]=None): NativeToJsonUnapplyResult = {
    val inputData = data.asInstanceOf[NativeToJsonApplyParam[T]]
    NativeToJsonUnapplyResult(convertJsonToNativeMsg[T](inputData.data.toString)(inputData.rjf))
  }
}

