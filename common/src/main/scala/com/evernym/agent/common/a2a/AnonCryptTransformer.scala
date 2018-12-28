package com.evernym.agent.common.a2a

import com.evernym.agent.common.wallet.{WalletAPI, WalletInfo}

case class AnonCryptApplyParam(keyInfo: KeyInfo, data: Array[Byte], walletInfo: WalletInfo) extends ApplyParam
case class AnonCryptUnapplyParam(keyInfo: KeyInfo, data: Array[Byte], walletInfo: WalletInfo) extends UnapplyParam

case class AnonCryptApplyResult(result: Array[Byte]) extends ApplyResult
case class AnonCryptUnapplyResult(result: Array[Byte]) extends UnapplyResult


class AnonCryptoTransformer (walletAPI: WalletAPI)
  extends Transformer[
    AnonCryptApplyParam, AnonCryptApplyResult,
    AnonCryptUnapplyParam, AnonCryptUnapplyResult] {

  override def apply[T, P](param: AnonCryptApplyParam)
                          (implicit oi:  ImplicitParam[P]=null): AnonCryptApplyResult = {
    AnonCryptApplyResult(walletAPI.anonCrypt(param.keyInfo, param.data)(param.walletInfo))
  }

  override def unapply[T, P](param: AnonCryptUnapplyParam)
                            (implicit oi:  ImplicitParam[P]=null): AnonCryptUnapplyResult = {
    AnonCryptUnapplyResult(walletAPI.anonDecrypt(param.keyInfo, param.data)(param.walletInfo))
  }
}
