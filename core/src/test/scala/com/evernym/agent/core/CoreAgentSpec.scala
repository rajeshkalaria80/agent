package com.evernym.agent.core

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import com.evernym.agent.common.test.akka.AkkaTestBasic
import com.evernym.agent.api.{CommonParam, ConfigProvider}
import com.evernym.agent.common.actor.{InitAgent, JsonTransformationUtil}
import com.evernym.agent.common.test.client.TestClientConfigProvider
import com.evernym.agent.common.test.spec.RouteSpecCommon


object TestPlatform extends TestKit(AkkaTestBasic.system) {
  lazy val configProvider: ConfigProvider = TestClientConfigProvider
  implicit lazy val materializer: Materializer = ActorMaterializer()

  implicit lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)

  val platform = new Platform

  def route: Route = platform.akkaHttpTransportRouteParam.route
}


class CoreAgentSpec extends RouteSpecCommon with JsonTransformationUtil {

  lazy val testClient = new TestCoreAgentClient()
  lazy val route: Route = TestPlatform.route

  it should "respond to init agent api call" in {
    val req = InitAgent(testClient.myDIDDetail.DID, testClient.myDIDDetail.verKey)
    testClient.buildPostReq("/agent/init", req) ~> route ~> check {
      status shouldBe OK
      testClient.handleAgentCreatedRespMsg(responseAs[Array[Byte]])
    }
  }

  it should "respond to create pairwise key api call" in {
    val (didDetail, req) = testClient.buildCreatePairwiseKeyReq()
    testClient.buildPostAgentMsgReq(req) ~> route ~> check {
      status shouldBe OK
      testClient.handlePairwiseKeyCreatedRespMsg(didDetail.DID, responseAs[Array[Byte]])
    }
  }

  it should "respond to get owner agent detail api call" in {
    val req = testClient.buildGetOwnerAgentDetailReq(testClient.myAgentDetail.id)
    testClient.buildPostAgentMsgReq(req) ~> route ~> check {
      status shouldBe OK
      testClient.handleOwnerAgentDetailRespMsg(responseAs[Array[Byte]])
    }
  }

  it should "respond to get owner agent detail for pairwise key api call" in {
    val req = testClient.buildGetOwnerAgentDetailReq(testClient.pairwiseDIDDetails.head._2.agentPairwiseId)
    testClient.buildPostAgentMsgReq(req) ~> route ~> check {
      status shouldBe OK
      testClient.handleOwnerAgentDetailRespMsg(responseAs[Array[Byte]])
    }
  }

}
