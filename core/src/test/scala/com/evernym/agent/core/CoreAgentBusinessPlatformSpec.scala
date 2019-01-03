package com.evernym.agent.core

import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import com.evernym.agent.common.test.akka.AkkaTestBasic
import com.evernym.agent.api.{CommonParam, ConfigProvider, GenericMsg, TransportMsg}
import com.evernym.agent.common.a2a.AuthCryptedMsg
import com.evernym.agent.common.actor.InitAgent
import com.evernym.agent.common.test.client.TestClientConfigProvider
import com.evernym.agent.common.test.spec.SpecCommon
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.core.platform.business.CoreAgentBusinessPlatform
import org.scalatest.{Assertion, AsyncFlatSpec}

import scala.concurrent.Future


object TestCoreAgent extends TestKit(AkkaTestBasic.system) {
  lazy val configProvider: ConfigProvider = TestClientConfigProvider
  implicit lazy val materializer: Materializer = ActorMaterializer()

  implicit lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)

  val platform = new CoreAgentBusinessPlatform(commonParam)
  platform.start()
}


class CoreAgentBusinessPlatformSpec extends AsyncFlatSpec with SpecCommon with CoreAgentClient {

  def sendToCoreAgent(msg: Any, f: Array[Byte] => Assertion): Future[Assertion] = {
    val futResp = TestCoreAgent.platform.handleMsg(TransportMsg("akka-http-transport", GenericMsg(msg)))
    futResp map { resp =>
      val acm = resp.asInstanceOf[AuthCryptedMsg]
      f(acm.payload)
    }
  }

  def sendAuthCryptedMsgToCoreAgent(msg: Array[Byte], f: Array[Byte] => Assertion): Future[Assertion] = {
    sendToCoreAgent(AuthCryptedMsg(msg), f)
  }

  it should "respond to init agent api call" in {
    val req = InitAgent(myDIDDetail.DID, myDIDDetail.verKey)
    sendToCoreAgent(req, { respPayload =>
      val respMsg = handleAgentCreatedRespMsg(respPayload)
      respMsg.`@type`.name shouldBe MSG_TYPE_AGENT_CREATED
    })
  }

  it should "respond to create pairwise key api call" in {
    val (didDetail, req) = buildCreatePairwiseKeyReq()
    sendAuthCryptedMsgToCoreAgent(req, { respPayload =>
      val respMsg = handlePairwiseKeyCreatedRespMsg(didDetail.DID, respPayload)
      respMsg.`@type`.name shouldBe MSG_TYPE_PAIRWISE_KEY_CREATED
    })
  }

  it should "respond to get owner agent detail api call" in {
    val req = buildGetOwnerAgentDetailReq(myUserAgentDetail.id)
    sendAuthCryptedMsgToCoreAgent(req, { respPayload =>
      val respMsg = handleOwnerAgentDetailRespMsg(respPayload)
      respMsg.`@type`.name shouldBe MSG_TYPE_OWNER_AGENT_DETAIL
    })
  }

  it should "respond to get owner agent detail for pairwise key api call" in {
    val req = buildGetOwnerAgentDetailReq(myUserAgentPairwiseDetails.head._2.agentPairwiseId)
    sendAuthCryptedMsgToCoreAgent(req, { respPayload =>
      val respMsg = handleOwnerAgentDetailRespMsg(respPayload)
      respMsg.`@type`.name shouldBe MSG_TYPE_OWNER_AGENT_DETAIL
    })
  }

}
