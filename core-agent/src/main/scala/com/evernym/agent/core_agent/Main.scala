package com.evernym.agent.core_agent

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api.{AgentMsgHandler, ConfigProvider, MsgOrchestrator, Transport}
import com.evernym.agent.core_agent.agent.{DefaultAgentMsgHandler, DefaultRoutingAgent}
import com.evernym.agent.core_agent.mediator.DefaultMsgOrchestrator
import com.evernym.agent.core_agent.transport.http.akka._
import com.typesafe.config.{Config, ConfigFactory}

class DefaultConfigProvider extends ConfigProvider {
  val config: Config = ConfigFactory.load()
}

object Main extends App {

  lazy val config: ConfigProvider = new DefaultConfigProvider()
  implicit lazy val system: ActorSystem = ActorSystem("agent")
  lazy val materializer: Materializer = ActorMaterializer()

  implicit lazy val param: AgentBaseParam = AgentBaseParam(config, system, materializer)

  lazy val agentMsgHandler: AgentMsgHandler = new DefaultAgentMsgHandler(param, new DefaultRoutingAgent(param))

  lazy val msgOrchestrator: MsgOrchestrator = new DefaultMsgOrchestrator(config, agentMsgHandler)

  implicit lazy val akkaHttpTransportParam: TransportHttpAkkaRouteParam =
    new DefaultTransportParamHttpAkka(config, msgOrchestrator)
  lazy val akkaHttpTransport: Transport = new DefaultTransportAkkaHttp(akkaHttpTransportParam)

  //start transport
  akkaHttpTransport.activate()

}

