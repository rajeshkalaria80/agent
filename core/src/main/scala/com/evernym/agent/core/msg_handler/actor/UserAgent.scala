package com.evernym.agent.core.msg_handler.actor

import akka.Done
import akka.actor.Props
import com.evernym.agent.common.a2a.{EncryptParam, GetVerKeyByDIDParam, KeyInfo}
import com.evernym.agent.common.actor._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import com.evernym.agent.core.actor.{AgentDetailSet, OwnerDetailSet}
import com.evernym.agent.core.common.{InitAgent, JsonTransformationUtil}


object UserAgent {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgent(agentCommonParam))
}

class UserAgent (val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon with JsonTransformationUtil {

  var ownerDetail: Option[DIDDetail] = None
  var agentDetail: Option[AgentDetail] = None

  def agentPairwiseVerKeyReq: String = agentDetail.map(_.verKey).
    getOrElse(throw new RuntimeException("agent not initialized yet"))

  def ownerDIDReq: String = ownerDetail.map(_.DID).
    getOrElse(throw new RuntimeException("agent not initialized yet"))

  def authCryptParam: EncryptParam = EncryptParam(
    KeyInfo(Left(agentPairwiseVerKeyReq)),
    KeyInfo(Right(GetVerKeyByDIDParam(ownerDIDReq, getKeyFromPool = false)))
  )

  override val receiveRecover: Receive = {
    case odw: OwnerDetailSet => ownerDetail = Option(DIDDetail(odw.DID, odw.verKey))
    case ai: AgentDetailSet => agentDetail = Option(AgentDetail(ai.agentID, ai.agentVerKey))
  }

  def initAgent(ia: InitAgent): Unit = {
    val wad = buildWalletAccessDetail(entityId)
    setWalletInfo(wad)
    agentActorCommonParam.walletAPI.createAndOpenWallet(wad)

    agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.DID, ia.verKey))
    writeAndApply(OwnerDetailSet(ia.DID, ia.verKey))

    val agentPairwiseNewKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)
    val agentDetail = AgentDetailSet(agentPairwiseNewKeyResult.DID, agentPairwiseNewKeyResult.verKey)
    writeAndApply(agentDetail)
    val acm = buildAgentCreatedRespMsg(agentDetail.agentID, agentDetail.agentVerKey)
    val respMsg = agentActorCommonParam.A2AAPI.authCryptMsg(authCryptParam, acm)
    sender ! respMsg
  }

  override val receiveCommand: Receive = {
    case _: InitAgent if agentDetail.isDefined => sender ! Done

    case ia: InitAgent => initAgent(ia)

    case GetAgentDetail => sender ! agentDetail

  }
}
