package com.evernym.agent.common.test.client


import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.a2a._
import com.evernym.agent.common.actor.DIDDetail
import com.evernym.agent.common.config.ConfigProviderBase
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.util.TransformationUtilBase
import com.evernym.agent.common.util.Util._
import com.evernym.agent.common.wallet._
import spray.json.RootJsonFormat


object TestClientConfigProvider extends ConfigProviderBase

case class TestTypeDetail(name: String, ver: String, fmt: Option[String]=None)

trait TestJsonTransformationUtil extends TransformationUtilBase {

  implicit val version: String = "1.0"

  implicit val typeDetailMsg: RootJsonFormat[TestTypeDetail] = jsonFormat3(TestTypeDetail.apply)

}

trait TestClientBase extends TestJsonTransformationUtil {

  val configProvider: ConfigProvider = TestClientConfigProvider
  val walletProvider: WalletProvider = new LibIndyWalletProvider(configProvider)
  val ledgerPoolMngr: LedgerPoolConnManager = new LedgerPoolConnManager(configProvider)
  val walletAPI: WalletAPI = new WalletAPI(walletProvider, ledgerPoolMngr)

  val defaultA2AAPI: AgentToAgentAPI = new DefaultAgentToAgentAPI(walletAPI)

  var walletAccessDetail: WalletAccessDetail = _
  implicit var walletInfo: WalletInfo = _

  var myDIDDetail: DIDDetail = _

  def init(): Unit = {
    val wn = "test-client-00000000000000000000"
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
    val json = convertNativeMsgToJson(payload)
    buildReq(HttpMethods.POST, path, HttpEntity(MediaTypes.`application/octet-stream`, payload))
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
        KeyInfo(Right(GetVerKeyByDIDParam(forAgentVerKey, getKeyFromPool = false)))
      )
    AuthCryptApplyParam(data, encryptParam, walletInfo)
  }

  def buildAuthDecryptParam(data: Array[Byte]): AuthCryptUnapplyParam = {
    val decryptParam = DecryptParam(KeyInfo(Left(myDIDDetail.verKey)))
    AuthCryptUnapplyParam(data, decryptParam, walletInfo)
  }

  def authDecryptRespMsg[T](rm: Array[Byte], decryptFromDID: String)(implicit rjf: RootJsonFormat[T])
  : T = {
    val param = DecryptParam(KeyInfo(Right(GetVerKeyByDIDParam(decryptFromDID, getKeyFromPool = false))))
    val prm = defaultA2AAPI.authDecrypt(buildAuthDecryptParam(rm))
    val msg: T = defaultA2AAPI.unpackMsg(prm)(Perhaps(rjf))
    println("### msg: " + msg)
    msg
  }

}
