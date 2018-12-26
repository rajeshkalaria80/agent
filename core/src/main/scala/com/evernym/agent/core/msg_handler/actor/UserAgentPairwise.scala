package com.evernym.agent.core.msg_handler.actor

import akka.Done
import akka.actor.Props
import com.evernym.agent.common.actor._
import com.evernym.agent.common.util.Util._
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import com.evernym.agent.core.actor.OwnerAgentPairwiseKeyDetailSet
import com.evernym.agent.core.common.InitAgentForPairwiseKey

import scala.concurrent.ExecutionContext.Implicits.global


object UserAgentPairwise {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgentPairwise(agentCommonParam))
}

class UserAgentPairwise(val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon {

  var ownerAgentKeyDetailOpt: Option[OwnerAgentKeyDetail] = None

  override val receiveRecover: Receive = {
    case odw: OwnerAgentPairwiseKeyDetailSet => ownerAgentKeyDetailOpt =
      Option(OwnerAgentKeyDetail(odw.ownerDID, odw.ownerDIDVerKey, odw.agentPairwiseVerKey))
      setWalletInfo(buildWalletAccessDetail(odw.agentId))
  }

  def ownerAgentDetailReq: OwnerAgentKeyDetail = ownerAgentKeyDetailOpt.
    getOrElse(throw new RuntimeException("agent not initialized yet"))

  def agentVerKeyReq: String = ownerAgentDetailReq.agentVerKey

  def ownerDIDReq: String = ownerAgentDetailReq.ownerDID

  def initAgentForPairwiseKey(ia: InitAgentForPairwiseKey): Unit = {
    val wad = buildWalletAccessDetail(ia.agentId)
    setWalletInfo(wad)

    agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.ownerPairwiseDID, ia.ownerPairwiseDIDVerKey))
    val agentPairwiseNewKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)

    val event = OwnerAgentPairwiseKeyDetailSet(ia.ownerPairwiseDID, ia.ownerPairwiseDIDVerKey,
      ia.agentId, agentPairwiseNewKeyResult.verKey)
    writeAndApply(event)

    val sndr = sender()
    val addRouteInfoSetFut = agentActorCommonParam.routingAgent.setRoute(entityId, buildRouteJson(ACTOR_TYPE_USER_AGENT_PAIRWISE_ACTOR))
    addRouteInfoSetFut.map {
      case Right(_: Any) =>
        sndr ! event
      case Left(e: Throwable) =>
        throw e
    }
  }

  override val receiveCommand: Receive = {
    case _: InitAgentForPairwiseKey if ownerAgentKeyDetailOpt.isDefined => sender ! Done

    case ia: InitAgentForPairwiseKey => initAgentForPairwiseKey(ia)

  }
}
