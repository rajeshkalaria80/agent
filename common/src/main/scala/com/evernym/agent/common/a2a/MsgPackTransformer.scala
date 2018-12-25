package com.evernym.agent.common.a2a

import com.evernym.agent.common.util.TransformationUtilBase
import org.velvia.MsgPack
import org.velvia.MsgPackUtils.unpackMap


case class MsgPackApplyParam(data: Map[String, Any]) extends ApplyParam
case class MsgPackApplyResult(result: Array[Byte]) extends ApplyResult

case class MsgPackUnapplyParam(data: Array[Byte]) extends UnapplyParam
case class MsgPackUnapplyResult(result: Map[String, Any]) extends UnapplyResult


class MsgPackTransformer
  extends Transformer[
    MsgPackApplyParam, MsgPackApplyResult,
    MsgPackUnapplyParam, MsgPackUnapplyResult] with TransformationUtilBase {

  override def apply[T, P](param: MsgPackApplyParam)(implicit pf: Perhaps[P]=null): MsgPackApplyResult = {
    MsgPackApplyResult(MsgPack.pack(param.data))
  }

  override def unapply[T, P](param: MsgPackUnapplyParam)(implicit pf: Perhaps[P]=null): MsgPackUnapplyResult = {
    MsgPackUnapplyResult(unpackMap(param.data).asInstanceOf[Map[String, Any]])
  }
}

