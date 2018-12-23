package com.evernym.agent.common.a2a

import java.util

import com.evernym.agent.common.util.TransformationUtilBase
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.exception.Exceptions._
import com.evernym.agent.common.wallet.{WalletAPI, WalletInfo}
import org.velvia.MsgPack
import org.velvia.MsgPackUtils.unpackMap
import spray.json._

case class A2AMsg(payload: Array[Byte])

case class GetVerKeyByDIDParam(DID: String, getKeyFromPool: Boolean)

case class KeyInfo(verKeyDetail: Either[String, GetVerKeyByDIDParam])

case class EncryptParam(fromKeyInfo: KeyInfo, forKeyInfo: KeyInfo)

case class DecryptParam(fromKeyInfo: KeyInfo)

trait A2AAPI {

  def msgPacker: MsgPacker = DefaultMsgPacker

  def authCryptMsg[T](param: EncryptParam, msg: T)(implicit walletInfo: WalletInfo, rjf: RootJsonFormat[T]): A2AMsg

  def authDecryptMsg[T](param: DecryptParam, msg: A2AMsg)(implicit walletInfo: WalletInfo, rjf: RootJsonFormat[T]): T
}


case class AuthCryptMsgParam(encryptInfo: EncryptParam, msg: A2AMsg)


trait MsgPacker {

  def packMsg[T](msg: T)(implicit rjf: RootJsonFormat[T]): Array[Byte]

  def unpackMsg[T](msg: Array[Byte])(implicit rjf: RootJsonFormat[T]): T
}

object DefaultMsgPacker extends MsgPacker with TransformationUtilBase {

  override def packMsg[T](msg: T)(implicit rjf: RootJsonFormat[T]): Array[Byte] = {
    try {
      val json: String = convertNativeMsgToJson(msg)
      val map = convertJsonToMap(json)
      MsgPack.pack(map)
    } catch {
      case e: IllegalArgumentException if e.getMessage.startsWith("requirement failed") =>
        throw new MissingReqField(TBR, s"required attribute not found (missing/empty/null): '$msg'")
    }
  }

  override  def unpackMsg[T](msg: Array[Byte])(implicit rjf: RootJsonFormat[T]): T = {
    val map = unpackMap(msg).asInstanceOf[Map[String, Any]]
    val json = convertMapToJson(map)
    convertJsonToNativeMsg[T](json.toString)
  }
}

class DefaultA2AAPI (walletAPI: WalletAPI) extends A2AAPI {

  override val msgPacker: MsgPacker = DefaultMsgPacker

  override def authCryptMsg[T](param: EncryptParam, msg: T)
                              (implicit walletInfo: WalletInfo, rjf: RootJsonFormat[T]): A2AMsg = {
    val packedMsg = msgPacker.packMsg(msg)
    A2AMsg(walletAPI.authCrypt(param, packedMsg))
  }

  override def authDecryptMsg[T](param: DecryptParam, msg: A2AMsg)
                                (implicit walletInfo: WalletInfo, rjf: RootJsonFormat[T]): T = {
    val authDecrypted = walletAPI.authDecrypt(param, msg.payload)
    msgPacker.unpackMsg(authDecrypted)
  }
}
