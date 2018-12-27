package com.evernym.agent.core.msg_handler.actor

import akka.Done
import akka.actor.Props
import akka.pattern.ask
import com.evernym.agent.common.a2a._
import com.evernym.agent.common.actor._
import com.evernym.agent.common.util.Util._
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.wallet.{CreateNewKeyParam, StoreTheirKeyParam}
import com.evernym.agent.core.actor._
import com.evernym.agent.core.common._
import spray.json.RootJsonFormat


import scala.concurrent.ExecutionContext.Implicits.global

object UserAgent {
  def props(agentCommonParam: AgentActorCommonParam) = Props(new UserAgent(agentCommonParam))
}

case class InitAgent(ownerDID: String, ownerDIDVerKey: String)


class UserAgent (val agentActorCommonParam: AgentActorCommonParam)
  extends PersistentActorBase with AgentActorCommon
    with JsonTransformationUtil with ActorRefResolver {

  var ownerDIDOpt: Option[String] = None
  var agentVerKeyOpt: Option[String] = None

  var ownerAgentPairwiseDIDS: Set[String] = Set.empty

  def ownerDIDReq: String = ownerDIDOpt.getOrElse(throwAgentNotInitializedYet())

  def agentVerKeyReq: String = agentVerKeyOpt.getOrElse(throwAgentNotInitializedYet())

  def getRouteJson: String = buildRouteJson(ACTOR_TYPE_USER_AGENT_ACTOR)

  override val receiveRecover: Receive = {
    case ods: OwnerDIDSet => ownerDIDOpt = Option(ods.DID)
    case oads: OwnerAgentDetailSet => agentVerKeyOpt = Option(oads.verKey)
    case opds: OwnerPairwiseDIDSet =>  ownerAgentPairwiseDIDS += opds.DID
  }

  def initAgent(ia: InitAgent): Unit = {
    val wad = buildWalletAccessDetail(entityId)
    setWalletInfo(wad)
    agentActorCommonParam.walletAPI.createAndOpenWallet(wad)
    agentActorCommonParam.walletAPI.storeTheirKey(StoreTheirKeyParam(ia.ownerDID, ia.ownerDIDVerKey))
    val agentKeyResult = agentActorCommonParam.walletAPI.createNewKey(CreateNewKeyParam())

    writeAndApply(OwnerDIDSet(ia.ownerDID))
    writeAndApply(OwnerAgentDetailSet(agentKeyResult.verKey))

    val sndr = sender()
    val addRouteInfoSetFut = agentActorCommonParam.routingAgent.setRoute(entityId, getRouteJson)
    addRouteInfoSetFut.map {
      case Right(_: Any) =>
        val acm = buildAgentCreatedRespMsg(entityId, agentKeyResult.verKey)
        val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(implParam[AgentCreatedRespMsg])
        sndr ! AuthCryptedMsg(respMsg)
      case Left(e: Throwable) =>
        throw e
    }

  }

  def createPairwiseKey(decryptedMsg: Array[Byte]): Unit = {
    val cpkr = agentToAgentAPI.unpackMsg[CreateAgentPairwiseKeyReqMsg,
      RootJsonFormat[CreateAgentPairwiseKeyReqMsg]](decryptedMsg)(implParam[CreateAgentPairwiseKeyReqMsg])
    if (ownerAgentPairwiseDIDS.contains(cpkr.fromDID)) {
      throw new RuntimeException("already added")
    } else {
      writeAndApply(OwnerPairwiseDIDSet(cpkr.fromDID))
      val agentPairwiseId = getNewEntityId
      val ar = agentActorCommonParam.commonParam.actorSystem.actorOf(UserAgentPairwise.props(agentActorCommonParam), agentPairwiseId)
      val iaFut = ar ? InitAgentForPairwiseKey(ownerDIDReq, entityId, cpkr.fromDID, cpkr.fromDIDVerKey)
      val sndr = sender()
      iaFut map {
        case oapds: OwnerAgentPairwiseDetailSet =>
          val acm = buildPairwiseKeyCreatedRespMsg(agentPairwiseId, oapds.agentPairwiseVerKey)
          val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(implParam[PairwiseKeyCreatedRespMsg])
          sndr ! AuthCryptedMsg(respMsg)
      }
    }
  }

  def handleGetOwnerAgentDetail(): Unit = {
    val acm = buildOwnerAgentDetailRespMsg(ownerDIDReq, entityId)
    val respMsg = agentToAgentAPI.packAndAuthCrypt(buildPackAndAuthCryptParam(acm))(implParam[OwnerAgentDetailRespMsg])
    sender ! AuthCryptedMsg(respMsg)
  }

  def handleFwdMsg(decryptedMsg: Array[Byte]): Unit = {
    val fwdMsg = agentToAgentAPI.unpackMsg[FwdMsg, RootJsonFormat[FwdMsg]](decryptedMsg)(implParam[FwdMsg])

    if (fwdMsg.fwd == entityId) {
      handleDecryptedMsg(fwdMsg.msg)
    } else {
      agentActorCommonParam.routingAgent.fwdMsgToAgent(fwdMsg.fwd, AuthCryptedMsg(fwdMsg.msg), sender)
    }
  }

  def handleDecryptedMsg(decryptedMsg: Array[Byte]): Unit = {
    val typedMsg = agentToAgentAPI.unpackMsg[AgentTypedMsg,
      RootJsonFormat[AgentTypedMsg]](decryptedMsg)(implParam[AgentTypedMsg])

    typedMsg.`@type` match {

      case TypeDetail(MSG_TYPE_CREATE_PAIRWISE_KEY, VERSION_1_0, _) => createPairwiseKey(decryptedMsg)

      case TypeDetail(MSG_TYPE_GET_OWNER_AGENT_DETAIL, VERSION_1_0, _) => handleGetOwnerAgentDetail()

      case _ => throw new RuntimeException(s"msg $typedMsg not supported")
    }
  }

  def handleAuthCryptedMsg(acm: AuthCryptedMsg): Unit = {
    val (typedMsg, decryptedMsg) = agentToAgentAPI.authDecryptAndUnpack[AgentTypedMsg,
      RootJsonFormat[AgentTypedMsg]](buildAuthDecryptParam(acm.payload))(implParam[AgentTypedMsg])

    typedMsg.`@type` match {

      case TypeDetail(MSG_TYPE_FWD, VERSION_1_0, _) => handleFwdMsg(decryptedMsg)

      case _ => throw new RuntimeException(s"msg $typedMsg not supported")
    }
  }

  override val receiveCommand: Receive = {
    case _: InitAgent if ownerDIDOpt.isDefined => sender ! Done

    case ia: InitAgent => initAgent(ia)

    case acm: AuthCryptedMsg =>
      handleAuthCryptedMsg(acm)

  }
}
