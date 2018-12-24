package com.evernym.agent.common.actor

import akka.persistence.PersistentActor
import com.evernym.agent.api.{CommonParam, RoutingAgent}
import com.evernym.agent.common.a2a.AgentToAgentAPI
import com.evernym.agent.common.wallet._
import com.evernym.agent.common.util.Util._

case class AgentDetail(id: String, verKey: String)

case class DIDDetail(DID: String, verKey: String)

case class AgentActorCommonParam(commonParam: CommonParam, routingAgent: RoutingAgent,
                                 walletConfig: WalletConfig, walletAPI: WalletAPI, agentToAgentAPI: AgentToAgentAPI)

trait AgentActorCommon {  this: PersistentActor =>

  case object GetAgentDetail

  implicit var walletInfo: WalletInfo = _

  def agentActorCommonParam: AgentActorCommonParam

  def buildWalletAccessDetail(actorEntityId: String): WalletAccessDetail = {
    //TODO: shall we keep wallet opened, any risk?
    val key = generateWalletKey(actorEntityId, agentActorCommonParam.walletAPI, agentActorCommonParam.commonParam.config)
    getWalletAccessDetail(actorEntityId, key, agentActorCommonParam.walletConfig,
      agentActorCommonParam.commonParam.config).copy(closeAfterUse = false)
  }

  def setWalletInfo(wad:  WalletAccessDetail): Unit = {
    walletInfo = WalletInfo(wad.walletName, Right(wad))
  }
}
