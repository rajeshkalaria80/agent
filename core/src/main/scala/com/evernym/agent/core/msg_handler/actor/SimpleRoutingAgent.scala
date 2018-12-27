package com.evernym.agent.core.msg_handler.actor

import akka.actor.Props
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.actor.PersistentActorBase
import com.evernym.agent.core.actor.RouteSet


//cmd
case class SetRoute(agentId: String, routeInfo: String)
case class GetRoute(agentId: String)


object SimpleRoutingAgent {
  def props(configProvider: ConfigProvider) = Props(new SimpleRoutingAgent(configProvider))
}


class SimpleRoutingAgent(configProvider: ConfigProvider) extends PersistentActorBase {

  var routes: Map[String, String] = Map.empty

  override def receiveRecover: Receive = {
    case rs: RouteSet =>
      routes += rs.agentId -> rs.routeJson
  }

  override def receiveCommand: Receive = {

    case sr: SetRoute => writeApplyAndSendItBack(RouteSet(sr.agentId, sr.routeInfo))

    case gr: GetRoute => sender ! routes.get(gr.agentId)
  }
}
