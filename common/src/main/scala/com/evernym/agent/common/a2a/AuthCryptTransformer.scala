package com.evernym.agent.common.a2a

import com.evernym.agent.common.wallet.{WalletAPI, WalletInfo}

case class GetVerKeyByDIDParam(DID: String, getKeyFromPool: Boolean)

case class KeyInfo(verKeyDetail: Either[String, GetVerKeyByDIDParam])


case class EncryptParam(fromKeyInfo: KeyInfo, forKeyInfo: KeyInfo)
case class AuthCryptApplyParam(data: Array[Byte],
                               encryptParam: EncryptParam, walletInfo: WalletInfo) extends ApplyParam
case class AuthCryptApplyResult(result: Array[Byte]) extends ApplyResult

case class DecryptParam(fromKeyInfo: KeyInfo)
case class AuthCryptUnapplyParam(data: Array[Byte],
                                 decryptParam: DecryptParam, walletInfo: WalletInfo) extends UnapplyParam
case class AuthCryptUnapplyResult(result: Array[Byte]) extends UnapplyResult


class AuthCryptoTransformer (walletAPI: WalletAPI)
  extends Transformer[
    AuthCryptApplyParam, AuthCryptApplyResult,
    AuthCryptUnapplyParam, AuthCryptUnapplyResult] {

  override def apply[T, P](param: AuthCryptApplyParam)
                          (implicit oi:  ImplicitParam[P]=null): AuthCryptApplyResult = {
    AuthCryptApplyResult(walletAPI.authCrypt(param.encryptParam, param.data)(param.walletInfo))
  }

  override def unapply[T, P](param: AuthCryptUnapplyParam)
                            (implicit oi:  ImplicitParam[P]=null): AuthCryptUnapplyResult = {
    AuthCryptUnapplyResult(walletAPI.authDecrypt(param.decryptParam, param.data)(param.walletInfo))
  }
}
