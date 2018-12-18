package com.evernym.agent.core_agent

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.core_agent.agent.{DefaultAgentMsgHandler, DefaultRoutingAgent}
import com.evernym.agent.core_agent.mediator.DefaultMsgOrchestrator
import com.evernym.agent.core_agent.transport.http.akka._


class DefaultConfigProvider extends ConfigProvider

object Main extends App {

  lazy val configProvider: ConfigProvider = new DefaultConfigProvider()
  lazy val system: ActorSystem = ActorSystem("agent")
  lazy val materializer: Materializer = ActorMaterializer()(system)

  implicit lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)


  lazy val agentMsgHandler: AgentMsgHandler =
    new DefaultAgentMsgHandler(commonParam, new DefaultRoutingAgent(commonParam))

  lazy val msgOrchestrator: MsgOrchestrator = new DefaultMsgOrchestrator(configProvider, agentMsgHandler)

  lazy val akkaHttpTransportRouteParam: TransportHttpAkkaRouteParam =
    new DefaultTransportParamHttpAkka(configProvider, msgOrchestrator)

  lazy val akkaHttpTransport: Transport = new DefaultTransportAkkaHttp(commonParam, akkaHttpTransportRouteParam)

  //start transport
  akkaHttpTransport.start()


  val defaultExtMngr = new DefaultExtensionManager(configProvider)
  defaultExtMngr.load()
  val internalApiExtWrapper = defaultExtMngr.getExtReq("agent-ext-akka-http-internal-api")
  internalApiExtWrapper.init(Option(commonParam))

}

