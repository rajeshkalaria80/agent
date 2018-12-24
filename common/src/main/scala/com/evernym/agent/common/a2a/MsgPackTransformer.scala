package com.evernym.agent.common.a2a

import com.evernym.agent.common.util.TransformationUtilBase
import org.velvia.MsgPack
import org.velvia.MsgPackUtils.unpackMap


class MsgPackApplyParam()
case class MsgPackApplyResult(msg: Array[Byte])

class MsgPackUnapplyParam()
case class MsgPackUnapplyResult(msg: Any)


class MsgPackTransformer
  extends Transformer[
    MsgPackApplyParam, MsgPackApplyResult,
    MsgPackUnapplyParam, MsgPackUnapplyResult] with TransformationUtilBase {

  override def apply[T](data: T, paramOpt: Option[MsgPackApplyParam]=None): MsgPackApplyResult = {
    MsgPackApplyResult(MsgPack.pack(data))
  }

  override def unapply[T](data: T, paramOpt: Option[MsgPackUnapplyParam]=None): MsgPackUnapplyResult = {
    MsgPackUnapplyResult(unpackMap(data.asInstanceOf[Array[Byte]]).asInstanceOf[Map[String, Any]])
  }
}

