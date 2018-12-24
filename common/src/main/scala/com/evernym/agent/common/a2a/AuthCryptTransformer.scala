package com.evernym.agent.common.a2a

import com.evernym.agent.common.wallet.{WalletAPI, WalletInfo}

case class GetVerKeyByDIDParam(DID: String, getKeyFromPool: Boolean)

case class KeyInfo(verKeyDetail: Either[String, GetVerKeyByDIDParam])


case class EncryptParam(fromKeyInfo: KeyInfo, forKeyInfo: KeyInfo)
case class AuthCryptApplyParam(encryptParam: EncryptParam, walletInfo: WalletInfo)
case class AuthCryptApplyResult(msg: Array[Byte])

case class DecryptParam(fromKeyInfo: KeyInfo)
case class AuthCryptUnapplyParam(decryptParam: DecryptParam, walletInfo: WalletInfo)
case class AuthCryptUnapplyResult(msg: Array[Byte])


class AuthCryptoTransformer (walletAPI: WalletAPI)
  extends Transformer[
    AuthCryptApplyParam, AuthCryptApplyResult,
    AuthCryptUnapplyParam, AuthCryptUnapplyResult] {

  override def apply[T](data: T, paramOpt: Option[AuthCryptApplyParam]): AuthCryptApplyResult = {
    val paramReq = paramOpt.getOrElse(throw new RuntimeException("required param missing"))
    AuthCryptApplyResult(walletAPI.authCrypt(paramReq.encryptParam, data.asInstanceOf[Array[Byte]])(paramReq.walletInfo))
  }

  override def unapply[T](data: T, paramOpt: Option[AuthCryptUnapplyParam]): AuthCryptUnapplyResult = {
    val paramReq = paramOpt.getOrElse(throw new RuntimeException("required param missing"))
    AuthCryptUnapplyResult(walletAPI.authDecrypt(paramReq.decryptParam, data.asInstanceOf[Array[Byte]])(paramReq.walletInfo))
  }
}
