package com.evernym.agent.common.actor

import akka.persistence.PersistentActor
import com.evernym.agent.api.{CommonParam, RoutingAgent}
import com.evernym.agent.common.a2a._
import com.evernym.agent.common.wallet._
import com.evernym.agent.common.util.Util._

case class AgentDetail(id: String, verKey: String)

case class OwnerAgentKeyDetail(ownerDID: String, ownerDIDVerKey: String, agentVerKey: String)

case class OwnerAgentPairwiseDetail(agentId: String, agentPairwiseVerKey: String)

case class AgentActorCommonParam(commonParam: CommonParam, routingAgent: RoutingAgent,
                                 walletConfig: WalletConfig, walletAPI: WalletAPI, agentToAgentAPI: AgentToAgentAPI)


trait AgentActorCommon extends JsonTransformationUtil {  this: PersistentActor =>

  case object GetAgentDetail

  implicit var walletInfo: WalletInfo = _

  def agentActorCommonParam: AgentActorCommonParam
  def param: CommonParam = agentActorCommonParam.commonParam
  def agentToAgentAPI: AgentToAgentAPI = agentActorCommonParam.agentToAgentAPI

  def ownerDIDReq: String
  def agentVerKeyReq: String

  def throwAgentNotInitializedYet() = throw new RuntimeException("agent not initialized yet")

  def buildWalletAccessDetail(actorEntityId: String): WalletAccessDetail = {
    //TODO: shall we keep wallet opened, any risk?
    val key = generateWalletKey(actorEntityId, agentActorCommonParam.walletAPI, agentActorCommonParam.commonParam.configProvider)
    getWalletAccessDetail(actorEntityId, key, agentActorCommonParam.walletConfig,
      agentActorCommonParam.commonParam.configProvider).copy(closeAfterUse = false)
  }

  def setWalletInfo(wad:  WalletAccessDetail): Unit = {
    walletInfo = WalletInfo(wad.walletName, Right(wad))
  }

  def getEncryptParam = EncryptParam(
    KeyInfo(Left(agentVerKeyReq)),
    KeyInfo(Right(GetVerKeyByDIDParam(ownerDIDReq, getKeyFromPool = false)))
  )

  def buildAuthCryptParam(data: Array[Byte]): AuthCryptApplyParam = {
    AuthCryptApplyParam(data, getEncryptParam, walletInfo)
  }

  def buildAuthDecryptParam(data: Array[Byte]): AuthCryptUnapplyParam = {
    val decryptParam = DecryptParam(KeyInfo(Left(agentVerKeyReq)))
    AuthCryptUnapplyParam(data, decryptParam, walletInfo)
  }

  def buildPackAndAuthCryptParam(data: Any): PackAndAuthCryptParam = {
    PackAndAuthCryptParam(data, getEncryptParam, walletInfo)
  }
}
