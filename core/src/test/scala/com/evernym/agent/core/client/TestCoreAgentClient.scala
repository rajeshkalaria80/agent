package com.evernym.agent.core.client

import com.evernym.agent.common.a2a.{EncryptParam, GetVerKeyByDIDParam, ImplicitParam, KeyInfo}
import com.evernym.agent.common.actor.AgentDetail
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.test.client.{DIDDetail, TestClientBase, TestFwdReqMsg, TestTypeDetail}
import com.evernym.agent.common.wallet.{CreateNewKeyParam, DefaultWalletAPI, WalletAPI}
import spray.json.RootJsonFormat


case class TestAgentCreatedRespMsg(`@type`: TestTypeDetail, agentId: String, agentVerKey: String)

case class TestCreatePairwiseKeyReqMsg(`@type`: TestTypeDetail, forDID: String, forDIDVerKey: String)

case class TestPairwiseKeyCreatedRespMsg(`@type`: TestTypeDetail, agentPairwiseId: String, agentPairwiseVerKey: String)

case class TestGetOwnerAgentDetailReqMsg(`@type`: TestTypeDetail)

case class TestOwnerAgentDetailRespMsg(`@type`: TestTypeDetail, ownerDID: String, agentId: String)


case class TestAgentPairwiseKeyDetail(myPairwiseVerKey: String, agentPairwiseId: String, agentPairwiseVerKey: String)


trait CoreAgentClient extends TestClientBase {

  lazy val walletAPI: WalletAPI = new DefaultWalletAPI(walletProvider, ledgerPoolMngr)
  override val agentMsgPath: String = "/agent/msg"

  implicit val testAgentCreatedRespMsg: RootJsonFormat[TestAgentCreatedRespMsg] = jsonFormat3(TestAgentCreatedRespMsg.apply)

  implicit val testCreatePairwiseKeyReqMsg: RootJsonFormat[TestCreatePairwiseKeyReqMsg] = jsonFormat3(TestCreatePairwiseKeyReqMsg.apply)
  implicit val testPairwiseKeyCreatedRespMsg: RootJsonFormat[TestPairwiseKeyCreatedRespMsg] = jsonFormat3(TestPairwiseKeyCreatedRespMsg.apply)

  implicit val testGetOwnerAgentDetailReqMsg: RootJsonFormat[TestGetOwnerAgentDetailReqMsg] = jsonFormat1(TestGetOwnerAgentDetailReqMsg.apply)
  implicit val testOwnerAgentDetailRespMsg: RootJsonFormat[TestOwnerAgentDetailRespMsg] = jsonFormat3(TestOwnerAgentDetailRespMsg.apply)


  var myUserAgentDetail: AgentDetail = _
  var myUserAgentPairwiseDetails: Map[String, TestAgentPairwiseKeyDetail] = Map.empty

  def getVerKeyForAuthCrypt(agentId: String): String = {
    val allRecord =
      Option(myUserAgentDetail).map(r => Map(r.id -> r.verKey)).getOrElse(Map.empty) ++
        myUserAgentPairwiseDetails.map(r => r._2.agentPairwiseId -> r._2.agentPairwiseVerKey)
    allRecord(agentId)
  }

  def createNewPairwiseKey(): DIDDetail = {
    val newKey = walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)
    val dd = DIDDetail(newKey.DID, newKey.verKey)
    myUserAgentPairwiseDetails += dd.DID -> TestAgentPairwiseKeyDetail(dd.verKey, null, null)
    dd
  }

  def myAgentVerKey: String = myUserAgentDetail.verKey

  def encryptParamForAgent: EncryptParam = EncryptParam (
    KeyInfo(Right(GetVerKeyByDIDParam(myDID, getKeyFromPool = false))),
    KeyInfo(Left(myAgentVerKey))
  )

  def setAgentDetail(id: String, verKey: String): Unit = {
    myUserAgentDetail = AgentDetail(id, verKey)
  }

  def setPairwiseAgentDetail(forMyDID: String, agentPairwiseId: String, agentPairwiseVerKey: String): Unit = {
    myUserAgentPairwiseDetails.get(forMyDID).foreach { r =>
      myUserAgentPairwiseDetails += forMyDID -> TestAgentPairwiseKeyDetail(r.myPairwiseVerKey, agentPairwiseId, agentPairwiseVerKey)
    }
  }

  def handleAgentCreatedRespMsg(rm: Array[Byte]): TestAgentCreatedRespMsg = {
    val ac = authDecryptAndUnpackRespMsg[TestAgentCreatedRespMsg](rm)
    setAgentDetail(ac.agentId, ac.agentVerKey)
    ac
  }

  def buildAuthCryptedMsg(forAgentId: String, origMsg: Array[Byte]): Array[Byte] = {
    defaultA2AAPI.authCrypt(buildAuthCryptParam(getVerKeyForAuthCrypt(forAgentId), origMsg))
  }

  def buildAuthCryptedFwdMsg(fwdTo: String, origMsg: Array[Byte]): Array[Byte] = {
    val finalPackedMsg = if (fwdTo == myUserAgentDetail.id) origMsg else {
      defaultA2AAPI.authCrypt(buildAuthCryptParam(myUserAgentPairwiseDetails.find(_._2.agentPairwiseId==fwdTo).get._2.agentPairwiseVerKey, origMsg))
    }
    val fwdReqMsg = TestFwdReqMsg(TestTypeDetail(MSG_TYPE_FWD, version), fwdTo, finalPackedMsg)
    val fwdPackedMsg = defaultA2AAPI.packMsg(fwdReqMsg)(ImplicitParam[RootJsonFormat[TestFwdReqMsg]](implicitly))
    defaultA2AAPI.authCrypt(buildAuthCryptParam(myUserAgentDetail.verKey, fwdPackedMsg))
  }

  def buildCreatePairwiseKeyReq(): (DIDDetail, Array[Byte]) = {
    val DIDDetail = createNewPairwiseKey()
    val nativeMsg = TestCreatePairwiseKeyReqMsg(
      TestTypeDetail(MSG_TYPE_CREATE_PAIRWISE_KEY, version), DIDDetail.DID, DIDDetail.verKey)
    val packedMsg = defaultA2AAPI.packMsg(nativeMsg)(ImplicitParam[RootJsonFormat[TestCreatePairwiseKeyReqMsg]](implicitly))
    val req = buildAuthCryptedMsg(myUserAgentDetail.id, packedMsg)
    (DIDDetail, req)
  }

  def handlePairwiseKeyCreatedRespMsg(forMyDID: String, rm: Array[Byte]): TestPairwiseKeyCreatedRespMsg = {
    val msg = authDecryptAndUnpackRespMsg[TestPairwiseKeyCreatedRespMsg](rm)
    setPairwiseAgentDetail(forMyDID, msg.agentPairwiseId, msg.agentPairwiseVerKey)
    msg
  }

  def buildGetOwnerAgentDetailReq(forAgentId: String): Array[Byte] = {
    val nativeMsg = TestGetOwnerAgentDetailReqMsg(TestTypeDetail(MSG_TYPE_GET_OWNER_AGENT_DETAIL, version))
    val packedMsg = defaultA2AAPI.packMsg(nativeMsg)(ImplicitParam[RootJsonFormat[TestGetOwnerAgentDetailReqMsg]](implicitly))
    val req = if (forAgentId == myUserAgentDetail.id) buildAuthCryptedMsg(forAgentId, packedMsg)
    else buildAuthCryptedFwdMsg(forAgentId, packedMsg)
    req
  }

  def handleOwnerAgentDetailRespMsg(rm: Array[Byte]): TestOwnerAgentDetailRespMsg = {
    val msg = authDecryptAndUnpackRespMsg[TestOwnerAgentDetailRespMsg](rm)
    println("### msg: " + msg)
    msg
  }

}
