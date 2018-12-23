package com.evernym.agent.core

import com.evernym.agent.common.a2a.{EncryptParam, GetVerKeyByDIDParam, KeyInfo}
import com.evernym.agent.common.actor.{AgentDetail, DIDDetail}
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.test.client.{TestClientBase, TestTypeDetail}
import spray.json.RootJsonFormat


case class TestAgentCreatedRespMsg(`@type`: TestTypeDetail, agentID: String, agentVerKey: String)

case class TestCreatePairwiseKeyReqMsg(`@type`: TestTypeDetail, fromDID: String, fromDIDVerKey: String)

case class TestPairwiseKeyCreatedRespMsg(`@type`: TestTypeDetail, agentPairwiseDID: String, agentPairwiseDIDVerKey: String)


class TestCoreAgentClient extends TestClientBase {

  implicit val agentCreatedRespMsg: RootJsonFormat[TestAgentCreatedRespMsg] = jsonFormat3(TestAgentCreatedRespMsg.apply)
  implicit val createPairwiseKeyResqMsg: RootJsonFormat[TestCreatePairwiseKeyReqMsg] = jsonFormat3(TestCreatePairwiseKeyReqMsg.apply)

  var myAgentDetail: AgentDetail = _

  def myAgentVerKey: String = myAgentDetail.verKey

  def encryptParamForAgent: EncryptParam = EncryptParam (
    KeyInfo(Right(GetVerKeyByDIDParam(myDID, getKeyFromPool = false))),
    KeyInfo(Left(myAgentVerKey))
  )

  def setAgentDetail(id: String, verKey: String): Unit = {
    myAgentDetail = AgentDetail(id, verKey)
  }

  def handleAgentCreatedRespMsg(rm: Array[Byte]): TestAgentCreatedRespMsg = {
    val ac = authDecryptRespMsg[TestAgentCreatedRespMsg](rm, myDID)
    setAgentDetail(ac.agentID, ac.agentVerKey)
    ac
  }

  def buildCreatePairwiseKeyReq(): Array[Byte] = {
    val DIDDetail = createNewPairwiseKey()
    val cpkr = TestCreatePairwiseKeyReqMsg(
      TestTypeDetail(MSG_TYPE_CREATE_PAIRWISE_KEY, version), DIDDetail.DID, DIDDetail.verKey)
    A2AAPI.authCryptMsg(encryptParamForAgent, cpkr).payload
  }

}
