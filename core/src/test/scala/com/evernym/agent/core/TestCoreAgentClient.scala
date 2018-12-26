package com.evernym.agent.core

import com.evernym.agent.common.a2a.{EncryptParam, GetVerKeyByDIDParam, ImplicitParam, KeyInfo}
import com.evernym.agent.common.actor.AgentDetail
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.test.client.{DIDDetail, TestClientBase, TestTypeDetail}
import com.evernym.agent.common.wallet.CreateNewKeyParam
import spray.json.RootJsonFormat


case class TestAgentCreatedRespMsg(`@type`: TestTypeDetail, agentId: String, agentVerKey: String)

case class TestCreatePairwiseKeyReqMsg(`@type`: TestTypeDetail, fromDID: String, fromDIDVerKey: String)

case class TestPairwiseKeyCreatedRespMsg(`@type`: TestTypeDetail, agentPairwiseId: String, agentPairwiseVerKey: String)

case class AgentPairwiseKeyDetail(myPairwiseVerKey: String, agentPairwiseId: String, agentPairwiseVerKey: String)

class TestCoreAgentClient extends TestClientBase {

  implicit val agentCreatedRespMsg: RootJsonFormat[TestAgentCreatedRespMsg] = jsonFormat3(TestAgentCreatedRespMsg.apply)
  implicit val createPairwiseKeyReqMsg: RootJsonFormat[TestCreatePairwiseKeyReqMsg] = jsonFormat3(TestCreatePairwiseKeyReqMsg.apply)

  implicit val pairwiseKeyCreatedRespMsg: RootJsonFormat[TestPairwiseKeyCreatedRespMsg] = jsonFormat3(TestPairwiseKeyCreatedRespMsg.apply)

  var myAgentDetail: AgentDetail = _
  var pairwiseDIDDetails: Map[String, AgentPairwiseKeyDetail] = Map.empty

  def createNewPairwiseKey(): DIDDetail = {
    val newKey = walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)
    val dd = DIDDetail(newKey.DID, newKey.verKey)
    pairwiseDIDDetails += dd.DID -> AgentPairwiseKeyDetail(dd.verKey, null, null)
    dd
  }

  def myAgentVerKey: String = myAgentDetail.verKey

  def encryptParamForAgent: EncryptParam = EncryptParam (
    KeyInfo(Right(GetVerKeyByDIDParam(myDID, getKeyFromPool = false))),
    KeyInfo(Left(myAgentVerKey))
  )

  def setAgentDetail(id: String, verKey: String): Unit = {
    myAgentDetail = AgentDetail(id, verKey)
  }

  def setPairwiseAgentDetail(forMyDID: String, agentPairwiseId: String, agentPairwiseVerKey: String): Unit = {
    pairwiseDIDDetails.get(forMyDID).foreach { r =>
      pairwiseDIDDetails += forMyDID -> AgentPairwiseKeyDetail(r.myPairwiseVerKey, agentPairwiseId, agentPairwiseVerKey)
    }
  }

  def handleAgentCreatedRespMsg(rm: Array[Byte]): TestAgentCreatedRespMsg = {
    val ac = authDecryptAndUnpackRespMsg[TestAgentCreatedRespMsg](rm, myDID)
    setAgentDetail(ac.agentId, ac.agentVerKey)
    ac
  }

  def buildCreatePairwiseKeyReq(): (DIDDetail, Array[Byte]) = {
    val DIDDetail = createNewPairwiseKey()
    val cpkr = TestCreatePairwiseKeyReqMsg(
      TestTypeDetail(MSG_TYPE_CREATE_PAIRWISE_KEY, version), DIDDetail.DID, DIDDetail.verKey)
    val cpkrPackedMsg = defaultA2AAPI.packMsg(cpkr)(ImplicitParam[RootJsonFormat[TestCreatePairwiseKeyReqMsg]](implicitly))
    val req = defaultA2AAPI.authCrypt(buildAuthCryptParam(myAgentDetail.verKey, cpkrPackedMsg))
    (DIDDetail, req)
  }

  def handlePairwiseKeyCreatedRespMsg(forMyDID: String, rm: Array[Byte]): TestPairwiseKeyCreatedRespMsg = {
    val ac = authDecryptAndUnpackRespMsg[TestPairwiseKeyCreatedRespMsg](rm, myDID)
    setPairwiseAgentDetail(forMyDID, ac.agentPairwiseId, ac.agentPairwiseVerKey)
    ac
  }

}
