package com.evernym.agent.common.a2a


import java.util

import com.evernym.agent.common.wallet.{WalletAPI, WalletInfo}


case class AuthCryptedMsg(payload: Array[Byte])


case class PackAndAuthCryptParam(data: Any, encryptParam: EncryptParam, walletInfo: WalletInfo)


trait AgentToAgentAPI {

  def authCrypt(param: AuthCryptApplyParam): Array[Byte]

  def authDecrypt(param: AuthCryptUnapplyParam): Array[Byte]

  def packMsg[T, P](data: T)(implicit oi:  ImplicitParam[P]=null): Array[Byte]

  def unpackMsg[T, P](data: Array[Byte])(implicit oi:  ImplicitParam[P]=null): T

  def unpackMsgPartial[T, P](data: Array[Byte])(implicit oi:  ImplicitParam[P]=null): (T, Array[Byte])

  def packAndAuthCrypt[T, P](param: PackAndAuthCryptParam)(implicit oi:  ImplicitParam[P]=null): Array[Byte]

  def authDecryptAndUnpack[T, P](param: AuthCryptUnapplyParam)(implicit oi:  ImplicitParam[P]=null): (T, Array[Byte])

}


class DefaultAgentToAgentAPI(walletAPI: WalletAPI) extends AgentToAgentAPI {

  private val msgPackTransformer = new MsgPackTransformer()
  private val jsonToMapTransformer = new JsonToMapTransformer()
  private val nativeToJsonTransformer = new NativeToJsonTransformer()
  private val authCryptoTransformer = new AuthCryptoTransformer(walletAPI)

  override def authCrypt(param: AuthCryptApplyParam): Array[Byte] = {
    applyAuthCryptTransformation(param)
  }

  override def authDecrypt(param: AuthCryptUnapplyParam): Array[Byte] = {
    unapplyAuthCryptTransformation(param)
  }

  private def applyAuthCryptTransformation[T](param: AuthCryptApplyParam): Array[Byte] = {
    authCryptoTransformer.apply(param).result
  }

  private def unapplyAuthCryptTransformation[T](param: AuthCryptUnapplyParam): Array[Byte] = {
    authCryptoTransformer.unapply(param).result
  }

  private def applyMsgPackTransformation[T](data: Map[String, Any]): Array[Byte] = {
    msgPackTransformer.apply(MsgPackApplyParam(data)).result
  }

  private def unapplyMsgPackTransformation[T](data: Array[Byte]): Map[String, Any] = {
    msgPackTransformer.unapply(MsgPackUnapplyParam(data)).result
  }

  private def applyJsonToMapTransformation(data: String): Map[String, Any] = {
    jsonToMapTransformer.apply(JsonToMapApplyParam(data)).result
  }

  private def unapplyJsonToMapTransformation[T](data: Map[String, Any]): String = {
    jsonToMapTransformer.unapply(JsonToMapUnapplyParam(data)).result
  }

  private def applyNativeToJsonTransformation[T, P](data: T)(implicit oi:  ImplicitParam[P]=null): String = {
    nativeToJsonTransformer.apply(NativeToJsonApplyParam(data)).result
  }

  private def unapplyNativeToJsonTransformation[T, P](data: String)(implicit oi:  ImplicitParam[P]=null): T = {
    nativeToJsonTransformer.unapply[T, P](NativeToJsonUnapplyParam(data)).result.asInstanceOf[T]
  }

  override def packMsg[T, P](data: T)(implicit oi:  ImplicitParam[P]=null): Array[Byte] = {
    val nativeToJsonApplyResult = applyNativeToJsonTransformation(data)
    val jsonToMapApplyResult = applyJsonToMapTransformation(nativeToJsonApplyResult)
    applyMsgPackTransformation(jsonToMapApplyResult)
  }

  override def unpackMsg[T, P](data: Array[Byte])(implicit oi:  ImplicitParam[P]=null): T = {
    val msgPackUnapplyResult = unapplyMsgPackTransformation(data)
    val mapToJsonUnapplyResult = unapplyJsonToMapTransformation(msgPackUnapplyResult)
    unapplyNativeToJsonTransformation[T, P](mapToJsonUnapplyResult)
  }

  override def packAndAuthCrypt[T, P](param: PackAndAuthCryptParam)
                                     (implicit oi:  ImplicitParam[P]=null): Array[Byte] = {
    val packedMsg = packMsg(param.data)
    authCrypt(AuthCryptApplyParam(packedMsg, param.encryptParam, param.walletInfo))
  }

  override def authDecryptAndUnpack[T, P](param: AuthCryptUnapplyParam)
                                         (implicit oi:  ImplicitParam[P]=null): (T, Array[Byte]) = {
    val decryptedMsg = authDecrypt(param)
    unpackMsgPartial[T, P](decryptedMsg)
  }

  def unpackMsgPartial[T, P](data: Array[Byte])(implicit oi:  ImplicitParam[P]=null): (T, Array[Byte]) = {
    val msgPackUnapplyResult = unapplyMsgPackTransformation(data)
    val mapToJsonUnapplyResult = unapplyJsonToMapTransformation(msgPackUnapplyResult)
    val res = unapplyNativeToJsonTransformation[T, P](mapToJsonUnapplyResult)
    (res, data)
  }

}
