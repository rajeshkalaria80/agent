package com.evernym.agent.core

import akka.http.scaladsl.server.Route
import com.evernym.agent.common.test.akka.AkkaTestBasic
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.test.spec.RouteSpecCommon
import com.evernym.agent.core.common.{AgentCreatedRespMsg, InitAgent, JsonTransformationUtil}
import com.evernym.agent.core.config.DefaultConfigProvider


object TestPlatform extends TestKit(AkkaTestBasic.system) with Platform {
  lazy val configProvider: ConfigProvider = DefaultConfigProvider
  implicit lazy val materializer: Materializer = ActorMaterializer()
}


class CoreAgentSpec extends RouteSpecCommon with JsonTransformationUtil {

  val route: Route = TestPlatform.akkaHttpTransportRouteParam.route

  it should "respond to init agent api call" in {
    val aiReq = InitAgent(testClient.myDIDDetail.DID, testClient.myDIDDetail.verKey)
    testClient.buildPostReq("/agent/init", aiReq) ~> route ~> check {
      status shouldBe OK
      testClient.handleRespMsg[AgentCreatedRespMsg](responseAs[Array[Byte]])
    }
  }

}
