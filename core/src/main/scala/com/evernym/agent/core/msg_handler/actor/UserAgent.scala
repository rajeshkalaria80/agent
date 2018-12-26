package com.evernym.agent.core.msg_handler.actor

import akka.Done
import akka.actor.Props
import akka.pattern.ask
import com.evernym.agent.common.a2a._
import com.evernym.agent.common.actor._
import com.evernym.agent.common.util.Util._
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import com.evernym.agent.core.actor._
import com.evernym.agent.core.common._
import spray.json.RootJsonFormat


import scala.concurrent.ExecutionContext.Implicits.global

object UserAgent {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgent(agentCommonParam))
}

class UserAgent (val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon
    with JsonTransformationUtil with ActorRefResolver {

  var ownerAgentKeyDetailOpt: Option[OwnerAgentKeyDetail] = None

  var ownerAgentPairwiseDIDS: Set[String] = Set.empty

  def ownerAgentDetailReq: OwnerAgentKeyDetail = ownerAgentKeyDetailOpt.getOrElse(throw new RuntimeException("agent not initialized yet"))

  def agentVerKeyReq: String = ownerAgentDetailReq.agentVerKey

  def ownerDIDReq: String = ownerAgentDetailReq.ownerDID

  override val receiveRecover: Receive = {
    case odw: OwnerAgentKeyDetailSet => ownerAgentKeyDetailOpt = Option(OwnerAgentKeyDetail(odw.ownerDID, odw.ownerDIDVerKey, odw.agentVerKey))
    case opka: OwnerAgentPairwiseKeyDetailSet => ownerAgentPairwiseDIDS += opka.ownerDID
  }

  def initAgent(ia: InitAgent): Unit = {
    val wad = buildWalletAccessDetail(entityId)
    setWalletInfo(wad)
    agentActorCommonParam.walletAPI.createAndOpenWallet(wad)
    agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.ownerDID, ia.ownerDIDVerKey))
    val agentKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)

    writeAndApply(OwnerAgentKeyDetailSet(ia.ownerDID, ia.ownerDIDVerKey, agentKeyResult.verKey))

    val sndr = sender()
    val addRouteInfoSetFut = agentActorCommonParam.routingAgent.setRoute(entityId, buildRouteJson(ACTOR_TYPE_USER_AGENT_ACTOR))
    addRouteInfoSetFut.map {
      case Right(_: Any) =>
        val acm = buildAgentCreatedRespMsg(entityId, agentKeyResult.verKey)
        val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(implParam[AgentCreatedRespMsg])
        sndr ! A2AMsg(respMsg)
      case Left(e: Throwable) =>
        throw e
    }

  }

  def createPairwiseKey(cpkr: CreateAgentPairwiseKeyReqMsg): Unit = {
    if (ownerAgentPairwiseDIDS.contains(cpkr.fromDID)) {
      throw new RuntimeException("already added")
    } else {
      writeAndApply(OwnerAgentPairwiseKeyDetailSet(cpkr.fromDID))
      val agentPairwiseId = getNewEntityId
      val ar = agentActorCommonParam.commonParam.actorSystem.actorOf(UserAgentPairwise.props(agentActorCommonParam), agentPairwiseId)
      val iaFut = ar ? InitAgentForPairwiseKey(cpkr.fromDID, cpkr.fromDIDVerKey, entityId)
      val sndr = sender()
      iaFut map {
        case oapkds: OwnerAgentPairwiseKeyDetailSet =>
          val acm = buildPairwiseKeyCreatedRespMsg(agentPairwiseId, oapkds.agentPairwiseVerKey)
          val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(implParam[PairwiseKeyCreatedRespMsg])
          sndr ! A2AMsg(respMsg)
      }
    }
  }

  def handleA2AMsg(a2aMsg: A2AMsg): Unit = {
    val (typedMsg, decryptedMsg) = agentToAgentAPI.authDecryptAndUnpack[AgentTypedMsg,
      RootJsonFormat[AgentTypedMsg]](buildAuthDecryptParam(a2aMsg.payload))(implParam[AgentTypedMsg])
    println("### typedMsg: " + typedMsg)
    typedMsg.`@type` match {
      case TypeDetail(MSG_TYPE_CREATE_PAIRWISE_KEY, "1.0", _) =>
        val actualMsg = agentToAgentAPI.unpackMsg[CreateAgentPairwiseKeyReqMsg,
          RootJsonFormat[CreateAgentPairwiseKeyReqMsg]](decryptedMsg)(implParam[CreateAgentPairwiseKeyReqMsg])
        createPairwiseKey(actualMsg)
      case _ => throw new RuntimeException(s"msg $typedMsg not supported")
    }
  }

  override val receiveCommand: Receive = {
    case _: InitAgent if ownerAgentKeyDetailOpt.isDefined => sender ! Done

    case ia: InitAgent => initAgent(ia)

    case a2a: A2AMsg => handleA2AMsg(a2a)

  }
}
