package com.evernym.agent.core_agent

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.core_agent.agent.{DefaultAgentMsgHandler, DefaultRoutingAgent}
import com.evernym.agent.core_agent.mediator.DefaultMsgOrchestrator
import com.evernym.agent.core_agent.transport.http.akka._
import com.typesafe.config.{Config, ConfigFactory}

class DefaultConfigProvider extends ConfigProvider {
  val config: Config = ConfigFactory.load()
}

object Main extends App {

  lazy val config: ConfigProvider = new DefaultConfigProvider()
  lazy val system: ActorSystem = ActorSystem("agent")
  lazy val materializer: Materializer = ActorMaterializer()(system)

  implicit lazy val transportParam: CommonParam = CommonParam(config, system, materializer)

  lazy val agentMsgHandler: AgentMsgHandler =
    new DefaultAgentMsgHandler(transportParam, new DefaultRoutingAgent(transportParam))

  lazy val msgOrchestrator: MsgOrchestrator = new DefaultMsgOrchestrator(config, agentMsgHandler)

  lazy val akkaHttpTransportRouteParam: TransportHttpAkkaRouteParam =
    new DefaultTransportParamHttpAkka(config, msgOrchestrator)

  lazy val akkaHttpTransport: Transport = new DefaultTransportAkkaHttp(transportParam, akkaHttpTransportRouteParam)

  //start transport
  akkaHttpTransport.start()

}

