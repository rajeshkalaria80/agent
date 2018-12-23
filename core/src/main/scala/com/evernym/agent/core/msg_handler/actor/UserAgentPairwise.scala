package com.evernym.agent.core.msg_handler.actor

import akka.Done
import akka.actor.Props
import com.evernym.agent.common.actor._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import com.evernym.agent.core.actor.{AgentDetailSet, OwnerDetailSet}
import com.evernym.agent.core.common.InitAgent


object UserAgentPairwise {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgentPairwise(agentCommonParam))
}

class UserAgentPairwise(val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon {

  var ownerDetail: Option[DIDDetail] = None
  var agentDetail: Option[AgentDetail] = None

  override val receiveRecover: Receive = {
    case odw: OwnerDetailSet => ownerDetail = Option(DIDDetail(odw.DID, odw.verKey))
    case ai: AgentDetailSet => agentDetail = Option(AgentDetail(ai.id, ai.verKey))
  }

  override val receiveCommand: Receive = {
    case _: InitAgent if agentDetail.isDefined => sender ! Done

    case ia: InitAgent =>
      val wad = buildWalletAccessDetail(entityId)
      setWalletInfo(wad)
      agentActorCommonParam.walletAPI.createAndOpenWallet(wad)

      agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.DID, ia.verKey))
      writeAndApply(OwnerDetailSet(ia.DID, ia.verKey))

      val agentPairwiseNewKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())(walletInfo)
      writeApplyAndSendItBack(AgentDetailSet(agentPairwiseNewKeyResult.DID, agentPairwiseNewKeyResult.verKey))

    case GetAgentDetail => sender ! agentDetail

  }
}
