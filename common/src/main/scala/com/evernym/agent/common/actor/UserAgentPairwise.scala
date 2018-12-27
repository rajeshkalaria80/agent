package com.evernym.agent.common.actor

import akka.Done
import akka.actor.Props
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.a2a.AuthCryptedMsg
import com.evernym.agent.common.util.Util._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global


object UserAgentPairwise {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgentPairwise(agentCommonParam))
}

class UserAgentPairwise(val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon with JsonTransformationUtil {

  var ownerDIDOpt: Option[String] = None
  var ownerPairwiseDIDOpt: Option[String] = None
  var ownerAgentPairwiseDetail: Option[OwnerAgentPairwiseDetail] = None

  def getRouteJson: String = buildRouteJson(ACTOR_TYPE_USER_AGENT_PAIRWISE_ACTOR)

  def agentVerKeyReq: String = ownerAgentPairwiseDetail.map(_.agentPairwiseVerKey).getOrElse(throwAgentNotInitializedYet())

  def ownerDIDReq: String = ownerDIDOpt.getOrElse(throwAgentNotInitializedYet())

  def ownerPairwiseDIDReq: String = ownerPairwiseDIDOpt.getOrElse(throwAgentNotInitializedYet())

  override val receiveRecover: Receive = {
    case ods: OwnerDIDSet => ownerDIDOpt = Option(ods.DID)
    case opds: OwnerPairwiseDIDSet => ownerPairwiseDIDOpt = Option(opds.DID)
    case oapds: OwnerAgentPairwiseDetailSet =>
      ownerAgentPairwiseDetail = Option(OwnerAgentPairwiseDetail(oapds.agentId, oapds.agentPairwiseVerKey))
      setWalletInfo(buildWalletAccessDetail(oapds.agentId))
  }

  def initAgentForPairwiseKey(ia: InitAgentForPairwiseKey): Unit = {
    val wad = buildWalletAccessDetail(ia.agentId)
    setWalletInfo(wad)

    agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.ownerPairwiseDID, ia.ownerPairwiseDIDVerKey))
    val agentPairwiseNewKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())

    writeAndApply(OwnerDIDSet(ia.ownerDID))
    writeAndApply(OwnerPairwiseDIDSet(ia.ownerPairwiseDID))
    val event = OwnerAgentPairwiseDetailSet(ia.agentId, agentPairwiseNewKeyResult.verKey)
    writeAndApply(event)

    val sndr = sender()
    val addRouteInfoSetFut = agentActorCommonParam.routingAgent.setRoute(entityId, getRouteJson)
    addRouteInfoSetFut.map {
      case Right(_: Any) => sndr ! event
      case Left(e: Throwable) => throw e
    }
  }

  def handleGetOwnerAgentDetail(): Unit = {
    val acm = buildOwnerAgentDetailRespMsg(ownerDIDReq, entityId)
    val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(implParam[OwnerAgentDetailRespMsg])
    sender ! AuthCryptedMsg(respMsg)
  }

  def handleAuthCryptedMsg(acm: AuthCryptedMsg): Unit = {
    val (typedMsg, _) = agentToAgentAPI.authDecryptAndUnpack[AgentTypedMsg,
      RootJsonFormat[AgentTypedMsg]](buildAuthDecryptParam(acm.payload))(implParam[AgentTypedMsg])

    typedMsg.`@type` match {

      case TypeDetail(MSG_TYPE_GET_OWNER_AGENT_DETAIL, VERSION_1_0, _) => handleGetOwnerAgentDetail()

      case _ => throw new RuntimeException(s"msg $typedMsg not supported")
    }
  }

  override val receiveCommand: Receive = {

    case _: InitAgentForPairwiseKey if ownerDIDOpt.isDefined => sender ! Done

    case ia: InitAgentForPairwiseKey => initAgentForPairwiseKey(ia)

    case acm: AuthCryptedMsg => handleAuthCryptedMsg(acm)

  }
}
