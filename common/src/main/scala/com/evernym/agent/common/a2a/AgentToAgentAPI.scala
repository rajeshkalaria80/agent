package com.evernym.agent.common.a2a


import com.evernym.agent.common.wallet.WalletAPI


case class A2AMsg(payload: Array[Byte])


trait TransformationResult


trait AgentToAgentAPI {

  def applyAuthCryptTransformation[T](msg: T, param: AuthCryptApplyParam): AuthCryptApplyResult

  def unapplyAuthCryptTransformation[T](msg: T, param: AuthCryptUnapplyParam): AuthCryptUnapplyResult

  def applyMsgPackTransformation[T](msg: T): MsgPackApplyResult

  def unapplyMsgPackTransformation[T](msg: T): MsgPackUnapplyResult

  def applyNativeToJsonTransformation[T](msg: T): NativeToJsonApplyResult

  def unapplyNativeToJsonTransformation[T](msg: T): NativeToJsonUnapplyResult

}



class DefaultA2AAPI (walletAPI: WalletAPI) extends AgentToAgentAPI {

  val authCryptoTransformer = new AuthCryptoTransformer(walletAPI)
  val msgPackTransformer = new MsgPackTransformer()
  val nativeToJsonTransformer = new NativeToJsonTransformer()

  def applyAuthCryptTransformation[T](msg: T)(implicit param: AuthCryptApplyParam): AuthCryptApplyResult = {
    authCryptoTransformer.apply(msg, Option(param))
  }

  def unapplyAuthCryptTransformation[T](msg: T)(implicit param: AuthCryptUnapplyParam): AuthCryptUnapplyResult = {
    authCryptoTransformer.unapply(msg, Option(param))
  }

  def applyMsgPackTransformation[T](msg: T): MsgPackApplyResult = {
    msgPackTransformer.apply(msg)
  }

  def unapplyMsgPackTransformation[T](msg: T): MsgPackUnapplyResult = {
    msgPackTransformer.unapply(msg)
  }

  def applyNativeToJsonTransformation[T](msg: T): NativeToJsonApplyResult = {
    nativeToJsonTransformer.apply(msg)
  }

  def unapplyNativeToJsonTransformation[T](msg: T): NativeToJsonUnapplyResult

}
