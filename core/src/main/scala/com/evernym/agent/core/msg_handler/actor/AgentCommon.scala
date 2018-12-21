package com.evernym.agent.core.msg_handler.actor


import akka.persistence.PersistentActor
import com.evernym.agent.common.wallet._
import com.evernym.agent.common.util.Util._
import com.evernym.agent.core.AgentActorCommonParam


trait AgentCommon {  this: PersistentActor =>

  case object GetAgentDetail
  case class AgentDetail(id: String, verKey: String)

  case class OwnerDetail(DID: String, verKey: String)

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
