package com.evernym.agent.common.test.client


import java.util.UUID

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.a2a._
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.util.TransformationUtilBase
import com.evernym.agent.common.util.Util._
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.config.ConfigProviderBase
import com.evernym.agent.common.wallet._
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.RootJsonFormat

case class TestTypeDetail(name: String, ver: String, fmt: Option[String]=None)

case class TestFwdReqMsg(`@type`: TestTypeDetail, fwd: String, msg: Array[Byte])

trait TestJsonTransformationUtil extends TransformationUtilBase {

  implicit val testTypeDetailMsg: RootJsonFormat[TestTypeDetail] = jsonFormat3(TestTypeDetail.apply)
  implicit val testFwdMsg: RootJsonFormat[TestFwdReqMsg] = jsonFormat3(TestFwdReqMsg.apply)

}

case class DIDDetail(DID: String, verKey: String)

object TestClientConfigProvider extends ConfigProviderBase {
  val config: Config = ConfigFactory.load()
}

trait TestClientBase extends TestJsonTransformationUtil {

  lazy val configProvider: ConfigProvider = TestClientConfigProvider
  lazy val walletProvider: WalletProvider = new LibIndyWalletProvider(configProvider)
  lazy val ledgerPoolMngr: LedgerPoolConnManager = new LedgerPoolConnManager(configProvider)
  def walletAPI: WalletAPI

  lazy val defaultA2AAPI: AgentToAgentAPI = new DefaultAgentToAgentAPI(walletAPI)

  var walletAccessDetail: WalletAccessDetail = _
  implicit var walletInfo: WalletInfo = _

  var myDIDDetail: DIDDetail = _

  def agentMsgPath: String

  def init(): Unit = {
    val wn = UUID.randomUUID().toString.replace("-", "")
    val wc = buildWalletConfig(configProvider)
    val key = walletAPI.generateWalletKey(Option(wn))

    walletAccessDetail = WalletAccessDetail(wn, key, wc, closeAfterUse = false)
    walletInfo = WalletInfo(wn, Right(walletAccessDetail))

    walletAPI.walletProvider.delete(wn, key, wc)

    walletAPI.createAndOpenWallet(walletAccessDetail)
    val newKey = walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)
    myDIDDetail = DIDDetail(newKey.DID, newKey.verKey)
  }

  init()

  def buildReq(hm: HttpMethod, path: String, he: RequestEntity = HttpEntity.Empty): HttpRequest =  {
    val req = HttpRequest(
      method = hm,
      uri = path,
      entity = he
    )
    req.addHeader(RawHeader("X-Real-IP", "127.0.0.1"))
  }

  def buildPostReq[T](path: String, payload: T)(implicit rjf: RootJsonFormat[T]): HttpRequest = {
    val json = convertNativeMsgToJson(payload)
    buildReq(HttpMethods.POST, path, HttpEntity(MediaTypes.`application/json`, json))
  }

  def buildPostReq[T](path: String, payload: Array[Byte]): HttpRequest = {
    buildReq(HttpMethods.POST, path, HttpEntity(MediaTypes.`application/octet-stream`, payload))
  }

  def buildPostAgentMsgReq[T](payload: Array[Byte]): HttpRequest = {
    buildReq(HttpMethods.POST, agentMsgPath, HttpEntity(MediaTypes.`application/octet-stream`, payload))
  }

  def buildPostReq(path: String, he: RequestEntity = HttpEntity.Empty): HttpRequest =
    buildReq(HttpMethods.POST, path, he)

  def buildPutReq(path: String, he: RequestEntity = HttpEntity.Empty): HttpRequest =
    buildReq(HttpMethods.PUT, path, he)

  def buildGetReq(path: String): HttpRequest =  {
    buildReq(HttpMethods.GET, path)
  }

  def myDID: String = myDIDDetail.DID

  def buildAuthCryptParam(forAgentVerKey: String, data: Array[Byte]): AuthCryptApplyParam = {
    val encryptParam =
      EncryptParam(
        KeyInfo(Left(myDIDDetail.verKey)),
        KeyInfo(Left(forAgentVerKey))
      )
    AuthCryptApplyParam(data, encryptParam, walletInfo)
  }

  def buildAuthDecryptParam(data: Array[Byte]): AuthCryptUnapplyParam = {
    val decryptParam = DecryptParam(KeyInfo(Left(myDIDDetail.verKey)))
    AuthCryptUnapplyParam(data, decryptParam, walletInfo)
  }

  def authDecryptAndUnpackRespMsg[T](rm: Array[Byte])(implicit rjf: RootJsonFormat[T])
  : T = {
    val decryptedMsg = defaultA2AAPI.authDecrypt(buildAuthDecryptParam(rm))
    defaultA2AAPI.unpackMsg[T, RootJsonFormat[T]](decryptedMsg)(ImplicitParam(rjf))
  }

}

