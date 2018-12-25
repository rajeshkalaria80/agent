package com.evernym.agent.common.a2a

import com.evernym.agent.common.util.TransformationUtilBase

case class JsonToMapApplyParam(data: String) extends ApplyParam
case class JsonToMapApplyResult(result: Map[String, Any]) extends ApplyResult

case class JsonToMapUnapplyParam(data: Map[String, Any]) extends UnapplyParam
case class JsonToMapUnapplyResult(result: String) extends UnapplyResult


class JsonToMapTransformer
  extends Transformer[
    JsonToMapApplyParam, JsonToMapApplyResult,
    JsonToMapUnapplyParam, JsonToMapUnapplyResult] with TransformationUtilBase {

  override def apply[T, P](param: JsonToMapApplyParam)(implicit pf: Perhaps[P]=null): JsonToMapApplyResult = {
    JsonToMapApplyResult(convertJsonToMap(param.data))
  }

  override def unapply[T, P](param: JsonToMapUnapplyParam)(implicit pf: Perhaps[P]=null): JsonToMapUnapplyResult = {
    JsonToMapUnapplyResult(convertMapToJson(param.data).toString)
  }
}

