package com.evernym.agent.common.util

import java.time.zone.ZoneRulesException
import java.time.{DateTimeException, Instant, ZoneId, ZonedDateTime}
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.evernym.agent.api.{CommonParam, ConfigProvider}
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.config.DefaultConfigProvider
import com.evernym.agent.common.exception.Exceptions.InvalidValue
import com.evernym.agent.common.wallet.{DefaultWalletConfig, WalletAPI, WalletAccessDetail, WalletConfig}
import com.typesafe.scalalogging.Logger
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}


trait UtilBase {

  val UTC = "UTC"

  def getActorRefFromSelection(path: String, actorSystem: ActorSystem,
                               resolveTimeoutOpt: Option[Timeout] = None,
                               awaitDurationOpt: Option[FiniteDuration] = None)
                              (implicit config: ConfigProvider): ActorRef = {
    val awaitDuration = buildDuration(
      config, TBR, 10)
    val resolveTimeout = resolveTimeoutOpt.getOrElse(Timeout(awaitDuration))
    Await.result(actorSystem.actorSelection(path).resolveOne()(resolveTimeout), awaitDuration)
  }

  def getZoneId(id: String): ZoneId = {
    try {
      val shortIds = ZoneId.SHORT_IDS.asScala
      val actualId = shortIds.getOrElse(id, id)
      ZoneId.of(actualId)
    } catch {
      case _ @ (_:ZoneRulesException | _:DateTimeException) =>
        throw new InvalidValue(TBR, s"invalid zone id: '$id'")
    }
  }

  val getUTCZoneId: ZoneId = getZoneId(UTC)

  def getLoggerByClass[T](c: Class[T]): Logger = Logger(c)

  def getLoggerByName(n: String): Logger = Logger(n)

  def getTimeoutValue(config: ConfigProvider, confName: String, default: Int): Int = {
    config.getConfigIntOption(confName).getOrElse(default)
  }

  def buildDuration(config: ConfigProvider, confName: String, default: Int): FiniteDuration = {
    val timeOutInSec = getTimeoutValue(config, confName, default)
    Duration.create(timeOutInSec, TimeUnit.SECONDS)
  }

  def buildTimeout(config: ConfigProvider, confName: String, default: Int): Timeout = {
    Timeout(buildDuration(config, confName, default))
  }

  def getZonedDateTimeFromMillis(millis: Long)(implicit zoneId: ZoneId): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId)

  def sha256Hex(text: String, length: Option[Int] = None): String = {
    val res = DigestUtils.sha256Hex(text)
    length.map { l =>
      res.substring(0, l)
    }.getOrElse(res)
  }

  def getLedgerTxnProtocolVersion(configProvider: ConfigProvider): Int = {
    val supportedVersions = Set(LEDGER_TXN_PROTOCOL_V1, LEDGER_TXN_PROTOCOL_V2)
    configProvider.getConfigIntOption(TBR) match {
      case None => LEDGER_TXN_PROTOCOL_V2
      case Some(v: Int) if supportedVersions.contains(v) => v
      case Some(x) => throw new RuntimeException(s"ledger txn protocol version $x not yet supported")
    }
  }

  def buildWalletKeySeed(secret: String, salt: String): String = {
    //NOTE: This logic should not be changed unless we know its impact
    val seed = DigestUtils.sha512Hex(secret + salt)
    UUID.nameUUIDFromBytes(seed.getBytes).toString.replace("-", "")
  }

  def getWalletKeySeed(id: String, configProvider: ConfigProvider): String = {
    //NOTE: This logic should not be changed unless we know its impact
    val salt = configProvider.getConfigStringReq("agent.salt.wallet-encryption")
    buildWalletKeySeed(id, salt)
  }

  def generateWalletKey(entityId: String, walletAPI: WalletAPI, configProvider: ConfigProvider): String =
    walletAPI.generateWalletKey(Option(getWalletKeySeed(entityId, configProvider)))

  def getWalletName(entityId: String, configProvider: ConfigProvider): String = {
    //NOTE: This logic should not be changed unless we know its impact
    sha256Hex(entityId + configProvider.getConfigStringReq("agent.salt.wallet-name"))
  }

  def getWalletAccessDetail(entityId: String, key: String, walletConfig: WalletConfig,
                            configProvider: ConfigProvider): WalletAccessDetail =
    WalletAccessDetail(
      getWalletName(entityId, configProvider),
      key,
      walletConfig
    )

  def createWalletConfig(configProvider: ConfigProvider): WalletConfig = {
    val storagePathOpt = configProvider.getConfigStringOption("agent.libindy.wallet.storage-path")
    new DefaultWalletConfig(storagePathOpt)
  }

  def buildDurationInSeconds(seconds: Int): FiniteDuration = Duration.create(seconds, TimeUnit.SECONDS)

  def buildRouteJson(actorTypeId: Int): String = {
    s"""{"actorTypeId":$actorTypeId}"""
  }

  def getNewEntityId: String = UUID.randomUUID.toString

  def buildDefaultCommonParam(actorSystemName: String): CommonParam = {
    lazy val configProvider: ConfigProvider = DefaultConfigProvider
    lazy val actorSystem: ActorSystem = ActorSystem(actorSystemName)
    lazy val materializer: Materializer = ActorMaterializer()(actorSystem)
    CommonParam(configProvider, actorSystem, materializer)
  }
}

object Util extends UtilBase