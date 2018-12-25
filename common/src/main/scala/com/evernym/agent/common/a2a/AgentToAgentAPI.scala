package com.evernym.agent.common.a2a


import com.evernym.agent.common.wallet.WalletAPI


case class A2AMsg(payload: Array[Byte])


trait AgentToAgentAPI {

  def packMsg[T, P](data: T)(implicit pf: Perhaps[P]=null): Array[Byte]

  def unpackMsg[T, P](data: Array[Byte])(implicit pf: Perhaps[P]=null): T

  def authCrypt(param: AuthCryptApplyParam): Array[Byte]

  def authDecrypt(param: AuthCryptUnapplyParam): Array[Byte]
}


class DefaultAgentToAgentAPI(walletAPI: WalletAPI) extends AgentToAgentAPI {

  private val authCryptoTransformer = new AuthCryptoTransformer(walletAPI)
  private val msgPackTransformer = new MsgPackTransformer()
  private val nativeToJsonTransformer = new NativeToJsonTransformer()
  private val jsonToMapTransformer = new JsonToMapTransformer()

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

  private def applyNativeToJsonTransformation[T, P](data: T)(implicit pf: Perhaps[P]=null): String = {
    nativeToJsonTransformer.apply(NativeToJsonApplyParam(data)).result
  }

  private def unapplyNativeToJsonTransformation[T, P](data: String)(implicit pf: Perhaps[P]=null): Any = {
    nativeToJsonTransformer.unapply(NativeToJsonUnapplyParam(data)).result
  }

  private def applyJsonToMapTransformation(data: String): Map[String, Any] = {
    jsonToMapTransformer.apply(JsonToMapApplyParam(data)).result
  }

  private def unapplyJsonToMapTransformation[T](data: Map[String, Any]): String = {
    jsonToMapTransformer.unapply(JsonToMapUnapplyParam(data)).result
  }

  override def packMsg[T, P](data: T)(implicit pf: Perhaps[P]=null): Array[Byte] = {
    val nativeToJsonApplyResult = applyNativeToJsonTransformation(data)
    val jsonToMapApplyResult = applyJsonToMapTransformation(nativeToJsonApplyResult)
    applyMsgPackTransformation(jsonToMapApplyResult)
  }

  override def unpackMsg[T, P](data: Array[Byte])(implicit pf: Perhaps[P]=null): T = {
    val msgPackUnapplyResult = unapplyMsgPackTransformation(data)
    val mapToJsonUnapplyResult = unapplyJsonToMapTransformation(msgPackUnapplyResult)
    val res = unapplyNativeToJsonTransformation(mapToJsonUnapplyResult)
    res.asInstanceOf[T]
  }

  override def authCrypt(param: AuthCryptApplyParam): Array[Byte] = {
    applyAuthCryptTransformation(param)
  }

  override def authDecrypt(param: AuthCryptUnapplyParam): Array[Byte] = {
    unapplyAuthCryptTransformation(param)
  }
}
