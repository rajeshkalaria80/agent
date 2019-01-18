package com.evernym.agent.core.client

import akka.actor.ActorSystem
import com.evernym.agent.common.actor.{AgentJsonTransformationUtil, InitAgent}
import com.evernym.agent.common.test.akka.AkkaTestBasic
import com.evernym.agent.common.transport.{Endpoint, HttpRemoteMsgSendingSvc}
import com.evernym.agent.common.util.Util.buildDurationInSeconds
import com.typesafe.config.Config

import scala.concurrent.Await


object TestCoreAgentApiClient extends CoreAgentClient
  with HttpRemoteMsgSendingSvc with AgentJsonTransformationUtil {

  import spray.json._

  override val config: Config = AkkaTestBasic.getConfig
  override val actorSystem: ActorSystem = AkkaTestBasic.system
  implicit val endpoint: Endpoint = Endpoint("localhost", 6000, "agent/msg")

  private def sendGeneralMsgToCoreAgent[T](msg: T, path: String)(implicit rjf: RootJsonFormat[T]): Array[Byte] = {
    val futResp = sendAsyncMsgExpectingBinaryResponse(msg.toJson.toString)(endpoint.copy(path = path))
    val resp = Await.result(futResp, buildDurationInSeconds(5))
    resp match {
      case Right(bd: Array[Byte]) => bd
      case Left(e) => throw new RuntimeException(e)
    }
  }

  private def sendBinaryMsgToCoreAgent(msg: Array[Byte]): Array[Byte] = {
    val futResp = sendAsyncMsgExpectingBinaryResponse(msg)
    val resp = Await.result(futResp, buildDurationInSeconds(5))
    resp match {
      case Right(bd: Array[Byte]) => bd
      case Left(e) => throw new RuntimeException(e)
    }
  }

  private def sendInitMsg[T](msg: T, path: String, rmh: Array[Byte] => Unit)
                    (implicit rjf: RootJsonFormat[T]): Unit = {
    val resp = sendGeneralMsgToCoreAgent(msg, path)
    val p = resp.asInstanceOf[Array[Byte]]
    rmh(p)
  }

  private def sendBinaryMsg(msg: Array[Byte], rhm: Array[Byte] => Unit): Unit = {
    val resp = sendBinaryMsgToCoreAgent(msg)
    rhm(resp)
  }

  def sendCreateAgent(): Unit = {
    val req = InitAgent(myDIDDetail.DID, myDIDDetail.verKey)
    sendInitMsg(req, "agent/init", { respPayload =>
      handleAgentCreatedRespMsg(respPayload)
    })
  }

  def sendCreatePairwiseKeyForUser(): Unit = {
    val (didDetail, req) = buildCreatePairwiseKeyReq()
    sendBinaryMsg(req, { respPayload =>
      handlePairwiseKeyCreatedRespMsg(didDetail.DID, respPayload)
    })
  }

  def sendGetOwnerDetail(): Unit = {
    val req = buildGetOwnerAgentDetailReq(myUserAgentDetail.id)
    sendBinaryMsg(req, { respPayload =>
      handleOwnerAgentDetailRespMsg(respPayload)
    })
  }

  def sendGetOwnerDetailFromPairwiseKey(): Unit = {
    val req = buildGetOwnerAgentDetailReq(myUserAgentPairwiseDetails.head._2.agentPairwiseId)
    sendBinaryMsg(req, { respPayload =>
      handleOwnerAgentDetailRespMsg(respPayload)
    })
  }
}
