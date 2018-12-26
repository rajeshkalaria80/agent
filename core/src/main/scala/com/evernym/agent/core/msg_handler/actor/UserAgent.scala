package com.evernym.agent.core.msg_handler.actor

import akka.Done
import akka.actor.Props
import com.evernym.agent.common.a2a._
import com.evernym.agent.common.actor._
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import com.evernym.agent.core.actor.{AgentDetailSet, OwnerDetailSet}
import com.evernym.agent.core.common._
import spray.json.RootJsonFormat


object UserAgent {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgent(agentCommonParam))
}

class UserAgent (val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon with JsonTransformationUtil {

  var ownerDetail: Option[DIDDetail] = None
  var agentDetail: Option[AgentDetail] = None

  def agentVerKeyReq: String = agentDetail.map(_.verKey).
    getOrElse(throw new RuntimeException("agent not initialized yet"))

  def ownerDIDReq: String = ownerDetail.map(_.DID).
    getOrElse(throw new RuntimeException("agent not initialized yet"))


  override val receiveRecover: Receive = {
    case odw: OwnerDetailSet => ownerDetail = Option(DIDDetail(odw.DID, odw.verKey))
    case ai: AgentDetailSet => agentDetail = Option(AgentDetail(ai.id, ai.verKey))
  }

  def initAgent(ia: InitAgent): Unit = {
    val wad = buildWalletAccessDetail(entityId)
    setWalletInfo(wad)
    agentActorCommonParam.walletAPI.createAndOpenWallet(wad)

    agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.DID, ia.verKey))
    writeAndApply(OwnerDetailSet(ia.DID, ia.verKey))

    val agentKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)
    val agentDetail = AgentDetailSet(entityId, agentKeyResult.verKey)
    writeAndApply(agentDetail)

    val acm = buildAgentCreatedRespMsg(agentDetail.id, agentDetail.verKey)
    val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(ImplicitParam[RootJsonFormat[AgentCreatedRespMsg]](implicitly))
    sender ! A2AMsg(respMsg)
  }

  def handleA2AMsg(a2aMsg: A2AMsg): Unit = {
    val (unpackedMsg, decryptedMsg) = agentToAgentAPI.authDecryptAndUnpack[AgentTypedMsg, RootJsonFormat[AgentTypedMsg]](buildAuthDecryptParam(a2aMsg.payload))(ImplicitParam[RootJsonFormat[AgentTypedMsg]](implicitly))
    println("### unpackedMsg: " + unpackedMsg)
    unpackedMsg.`@type` match {
      case TypeDetail(MSG_TYPE_CREATE_PAIRWISE_KEY, "1.0", _) =>
        val actualMsg = agentToAgentAPI.unpackMsg[CreatePairwiseKeyReqMsg, RootJsonFormat[CreatePairwiseKeyReqMsg]](decryptedMsg)(ImplicitParam[RootJsonFormat[CreatePairwiseKeyReqMsg]](implicitly))
        println("### actualMsg: " + actualMsg)
    }
  }

  override val receiveCommand: Receive = {
    case _: InitAgent if agentDetail.isDefined => sender ! Done

    case ia: InitAgent => initAgent(ia)

    case a2a: A2AMsg => handleA2AMsg(a2a)

  }
}
