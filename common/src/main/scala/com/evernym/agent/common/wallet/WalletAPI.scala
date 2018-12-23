package com.evernym.agent.common.wallet

import com.evernym.agent.common.util.Util._
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutionException

import com.evernym.agent.common.exception.Exceptions._
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.a2a.{DecryptParam, EncryptParam, GetVerKeyByDIDParam}
import com.evernym.agent.common.exception.Exceptions
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.typesafe.scalalogging.Logger
import org.hyperledger.indy.sdk.InvalidStructureException
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.{Did, DidJSONParameters}
import org.hyperledger.indy.sdk.wallet.WalletItemAlreadyExistsException


case class WalletAccessDetail(walletName: String, encryptionKey: String,
                              walletConfig: WalletConfig, closeAfterUse: Boolean = true) {
  def getUniqueKey: String = sha256Hex(walletName + encryptionKey)
}

case class WalletInfo(name: String, walletBuilder: Either[WalletExt, WalletAccessDetail])

case class CreateNewKeyParam(DID: Option[String] = None, seed: Option[String] = None) {
  override def toString: String = {
    val redacted = seed.map(_ => "redacted")
    s"CreateNewKeyParam($DID, $redacted)"
  }
}
case class NewKeyCreated(DID: String, verKey: String)


case class StoreTheirKeyParam(theirDID: String, theirDIDVerKey: String)
case class TheirKeyCreated(DID: String, verKey: String)


class WalletAPI (val walletProvider: WalletProvider, ledgerPoolManager: LedgerPoolConnManager) {

  val logger: Logger = getLoggerByClass(classOf[WalletAPI])
  var wallets: Map[String, WalletExt] = Map.empty

  private def _openWallet(implicit wad: WalletAccessDetail): WalletExt = {
    try {
      walletProvider.open(wad.walletName, wad.encryptionKey, wad.walletConfig)
    } catch {
      case e: Throwable =>
        val err = s"error while opening wallet (wallet-name: ${wad.walletName}, " +
          s"thread-id: ${Thread.currentThread().getId}, " +
          s"error: ${Exceptions.getErrorMsg(e)})"
        throw new RuntimeException(err, e)
    }
  }

  private def _executeOpWithWallet[T](opContext: String, op: WalletExt => T)
                                     (implicit w: WalletExt): T = {
    val startTime = LocalDateTime.now
    logger.debug(s"libindy api call started ($opContext)")
    val result = op(w)
    val curTime = LocalDateTime.now
    val millis = ChronoUnit.MILLIS.between(startTime, curTime)
    logger.debug(s"libindy api call finished ($opContext), time taken (in millis): $millis")
    result
  }

  private def addToOpenedWalletIfReq(wad: WalletAccessDetail, w: WalletExt): Unit = {
    if (! wad.closeAfterUse) wallets += wad.getUniqueKey -> w
  }

  private def _executeOpWithWalletAccessDetail[T](opContext: String, openWalletIfNotExists: Boolean, op: WalletExt => T)
                                                 (implicit wad: WalletAccessDetail): T = {
    implicit val w: WalletExt =
      if (openWalletIfNotExists) wallets.getOrElse(wad.getUniqueKey, _openWallet)
      else wallets(wad.getUniqueKey)

    addToOpenedWalletIfReq(wad, w)
    try {
      _executeOpWithWallet(opContext, op)
    } finally {
      if (! wallets.contains(wad.getUniqueKey) && wad.closeAfterUse) {
        walletProvider.close(w)
        logger.debug(s"wallet successfully closed (detail => wallet-name: ${wad.walletName}, " +
          s"thread-id: ${Thread.currentThread().getId})")
      }
    }
  }

  def executeOpWithWalletInfo[T](opContext: String, openWalletIfNotExists: Boolean, op: WalletExt => T)
                                (implicit walletInfo: WalletInfo): T = {
    walletInfo.walletBuilder.fold(
      we => _executeOpWithWallet(opContext, op)(we),
      wad => _executeOpWithWalletAccessDetail(opContext, openWalletIfNotExists, op)(wad)
    )
  }

  def createAndOpenWallet(wad: WalletAccessDetail): WalletExt = {
    val startTime = LocalDateTime.now
    logger.debug(s"libindy api call started (create and open wallet)")
    val we = walletProvider.createAndOpen(wad.walletName, wad.encryptionKey, wad.walletConfig)
    addToOpenedWalletIfReq(wad, we)
    val curTime = LocalDateTime.now
    val millis = ChronoUnit.MILLIS.between(startTime, curTime)
    logger.debug(s"libindy api call finished (create and open wallet), time taken (in millis): $millis")
    we
  }

  def generateWalletKey(seedOpt: Option[String] = None): String =
    walletProvider.generateKey(seedOpt)

  def createNewKey(cnk: CreateNewKeyParam)(implicit walletInfo: WalletInfo):
  NewKeyCreated = {
    executeOpWithWalletInfo("create new key", openWalletIfNotExists=false, { we: WalletExt =>
      try {
        val DIDJson = new DidJSONParameters.CreateAndStoreMyDidJSONParameter(cnk.DID.orNull, cnk.seed.orNull, null, null)
        val didResult = Did.createAndStoreMyDid(we.wallet, DIDJson.toJson).get
        val result = NewKeyCreated(didResult.getDid, didResult.getVerkey)
        NewKeyCreated(result.DID, result.verKey)
      } catch {
        case e: ExecutionException =>
          e.getCause match {
            case e: InvalidStructureException =>
              throw new BadRequestError(TBR, e.getMessage)
            case e: Throwable =>
              logger.error("error while creating key, internal cause: " + Exceptions.getErrorMsg(e))
              throw new InternalServerError(
                TBR, s"unhandled error while creating new key")
          }
        case e: Throwable =>
          logger.error("error while creating key: " + Exceptions.getErrorMsg(e))
          throw new BadRequestError(TBR, "unhandled error while creating new key")
      }
    })
  }

  def storeTheirKey(stk: StoreTheirKeyParam, ignoreIfAlreadyExists: Boolean=false)
                   (implicit walletInfo: WalletInfo): TheirKeyCreated = {
    executeOpWithWalletInfo("store their key", openWalletIfNotExists=false, { we: WalletExt =>
      try {
        logger.debug("about to store their key => wallet name: " + walletInfo.name + ", key: " + stk)
        val DIDJson = s"""{\"did\":\"${stk.theirDID}\",\"verkey\":\"${stk.theirDIDVerKey}\"}"""
        Did.storeTheirDid(we.wallet, DIDJson).get
        val tkc = TheirKeyCreated(stk.theirDID, stk.theirDIDVerKey)
        logger.debug("their key stored => wallet name: " + walletInfo.name + ", tkc: " + tkc)
        tkc
      } catch {
        case e: Throwable if ignoreIfAlreadyExists && e.getCause.isInstanceOf[WalletItemAlreadyExistsException] =>
          TheirKeyCreated(stk.theirDID, stk.theirDIDVerKey)
        case e: Throwable =>
          logger.error("error while storing their key: " + Exceptions.getErrorMsg(e))
          throw new InternalServerError(TBR, s"unhandled error while storing their key")
      }
    })
  }

  def getVerKey(DID: String, walletExt: WalletExt, getKeyFromPool: Boolean,
                poolConnManager: LedgerPoolConnManager): String = {
    if (getKeyFromPool) Did.keyForDid(poolConnManager.getSafePoolConn, walletExt.wallet, DID).get
    else Did.keyForLocalDid(walletExt.wallet, DID).get
  }


  def getVerKeyFromWallet(verKeyDetail: Either[String, GetVerKeyByDIDParam])(implicit we: WalletExt): String = {
    verKeyDetail.fold (
      l => l,
      r => {
        getVerKey(r.DID, we, r.getKeyFromPool, ledgerPoolManager)
      }
    )
  }

  def authCrypt(param: EncryptParam, msg: Array[Byte])(implicit walletInfo: WalletInfo): Array[Byte] = {
    executeOpWithWalletInfo("auth crypt", openWalletIfNotExists=false, { implicit we: WalletExt =>
      val senderKey = getVerKeyFromWallet(param.fromKeyInfo.verKeyDetail)
      val recipKey = getVerKeyFromWallet(param.forKeyInfo.verKeyDetail)
      Crypto.authCrypt(we.wallet, senderKey, recipKey, msg).get
    })
  }

  def authDecrypt(param: DecryptParam, msg: Array[Byte])(implicit walletInfo: WalletInfo): Array[Byte] = {
    executeOpWithWalletInfo("auth decrypt", openWalletIfNotExists=false, { we: WalletExt =>
      val decryptFromVerKey = getVerKeyFromWallet(param.fromKeyInfo.verKeyDetail)(we)
      try {
        Crypto.authDecrypt(we.wallet, decryptFromVerKey, msg).get.getDecryptedMessage
      } catch {
        case e: ExecutionException =>
          e.getCause match {
            case _: InvalidStructureException =>
              throw new BadRequestError(TBR, "invalid encrypted box")
            case _: Throwable => throw new BadRequestError(TBR, "unhandled error while decrypting msg")
          }
        case _: Throwable => throw new BadRequestError(TBR, "unhandled error while decrypting msg")
      }
    })
  }

}
