package com.evernym.agent.common.wallet

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.exception.wallet.WalletExceptions._
import com.evernym.agent.common.libindy.LibIndyCommon
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.util.Util._
import com.typesafe.scalalogging.Logger
import org.hyperledger.indy.sdk.{IOException, InvalidStateException}
import org.hyperledger.indy.sdk.wallet._


trait WalletConfig {
  def buildConfig(id: String): String
  def buildCredentials(encKey: String): String
}

class DefaultWalletConfig (pathOpt: Option[String] = None) extends WalletConfig {


  override def buildConfig(id: String): String = {
    val storageConfigs = pathOpt.map { path =>
      s"""{
         |"path":"$path"
         }""".stripMargin
    }.getOrElse("""{}""")

    s"""{
       |"id": "$id",
       |"storage_type": "default",
       |"storage_config": $storageConfigs
       |}""".stripMargin
  }

  override def buildCredentials(encKey: String): String = {
    s"""{
       |"key": "$encKey",
       |"storage_credentials": {},
       |"key_derivation_method":"RAW"
       |}""".stripMargin
  }
}

trait WalletExt {
  def wallet: Wallet
}


trait WalletProvider {

  def generateKey(seedOpt: Option[String] = None): String

  def createAndOpen(id: String, encryptionKey: String, walletConfig: WalletConfig): WalletExt

  def create(id: String, encryptionKey: String, walletConfig: WalletConfig): Unit

  def open(id: String, encryptionKey: String, walletConfig: WalletConfig): WalletExt

  def checkIfWalletExists(id: String, encryptionKey: String, walletConfig: WalletConfig): Boolean

  def close(walletExt: WalletExt): Unit

  def delete(id: String, encryptionKey: String, walletConfig: WalletConfig): Unit
}


class LibIndyWalletExt (val wallet: Wallet) extends WalletExt

class LibIndyWalletProvider(val configProvider: ConfigProvider) extends LibIndyCommon with WalletProvider {
  val logger: Logger = getLoggerByClass(classOf[LibIndyWalletProvider])

  def generateKey(seedOpt: Option[String] = None): String = {
    val conf = seedOpt.map(seed => s"""{"seed":"$seed"}""").getOrElse("""{}""")
    Wallet.generateWalletKey(conf).get
  }

  def createAndOpen(id: String, encryptionKey: String, walletConfig: WalletConfig): LibIndyWalletExt = {
    create(id, encryptionKey, walletConfig)
    openWithMaxTries(id, encryptionKey, walletConfig)(1)
  }

  def openWithMaxTries(id: String, encryptionKey: String, walletConfig: WalletConfig)
                      (maxTryCount: Int, curTryCount: Int = 1): LibIndyWalletExt = {
    try {
      logger.debug(s"open wallet attempt started ($curTryCount/$maxTryCount)")
      open(id, encryptionKey, walletConfig)
    } catch {
      case _: WalletInvalidState if curTryCount < maxTryCount =>
        Thread.sleep(50)
        openWithMaxTries(id, encryptionKey, walletConfig)(maxTryCount, curTryCount+1)
    }
  }

  def create(id: String, encryptionKey: String,
             walletConfig: WalletConfig): Unit = {
    try {
      val startTime = LocalDateTime.now
      logger.debug(s"libindy api call started (create wallet)")
      Wallet.createWallet(
        walletConfig.buildConfig(id),
        walletConfig.buildCredentials(encryptionKey)).get
      val curTime = LocalDateTime.now
      val millis = ChronoUnit.MILLIS.between(startTime, curTime)
      logger.debug(s"libindy api call finished (create wallet), time taken (in millis): $millis")
    } catch {
      case e: Throwable if e.getCause.isInstanceOf[WalletExistsException] =>
        throw new WalletAlreadyExist(TBR, "wallet already exists with name: " + id)
    }
  }

  def open(id: String, encryptionKey: String, walletConfig: WalletConfig): LibIndyWalletExt = {
    try {
      val startTime = LocalDateTime.now
      logger.debug(s"libindy api call started (open wallet)")
      val wallet = Wallet.openWallet(walletConfig.buildConfig(id),
        walletConfig.buildCredentials(encryptionKey)).get
      val curTime = LocalDateTime.now
      val millis = ChronoUnit.MILLIS.between(startTime, curTime)
      logger.debug(s"libindy api call finished (open wallet), time taken (in millis): $millis")
      new LibIndyWalletExt(wallet)
    } catch {
      case e: Throwable if e.getCause.isInstanceOf[WalletAlreadyOpenedException] =>
        throw new WalletAlreadyOpened(TBR, s"wallet already opened: " + id)
      case e: Throwable if e.getCause.isInstanceOf[WalletNotFoundException] =>
        throw new WalletDoesNotExist(TBR, s"wallet does not exist: '$id'")
      case e: Throwable if e.getCause.isInstanceOf[IOException] &&
        e.getCause.asInstanceOf[IOException].getSdkErrorCode == 114 =>
        throw new WalletDoesNotExist(TBR, s"wallet/table does not exist: '$id'")
      case e: Throwable if e.getCause.isInstanceOf[InvalidStateException] =>
        throw new WalletInvalidState(TBR, s"error while opening wallet: '$id'")
      case e: Throwable =>
        throw new WalletNotOpened(TBR, s"error while opening wallet '$id': " + e.toString)
    }
  }

  def checkIfWalletExists(walletName: String, encryptionKey: String, walletConfig: WalletConfig): Boolean = {
    try {
      close(open(walletName, encryptionKey, walletConfig))
      true
    } catch {
      case _: WalletDoesNotExist =>
        false
    }

  }

  def close(walletExt: WalletExt): Unit = {
    walletExt.wallet.closeWallet()
  }

  def delete(id: String, encryptionKey: String, walletConfig: WalletConfig): Unit = {
    try {
      Wallet.deleteWallet(walletConfig.buildConfig(id),
        walletConfig.buildCredentials(encryptionKey))
    } catch {
      //TODO: shall we catch only specific exception?
      case e: Throwable => throw new WalletNotDeleted(TBR,
        s"error while deleting wallet '$id' : " + e.toString)
    }
  }

}
