package com.evernym.agent.core.msg_handler.actor

import akka.actor.Props
import com.evernym.agent.api.ConfigProvider


//cmd
case class SetRoute(agentID: String, routeInfo: String)
case class GetRoute(agentID: String)

object SimpleRoutingAgent {
  def props(configProvider: ConfigProvider) = Props(new SimpleRoutingAgent(configProvider))
}

class SimpleRoutingAgent(configProvider: ConfigProvider) extends PersistentActorBase {

  var routes: Set[RouteSet] = Set.empty

  override def receiveRecover: Receive = {
    case rs: RouteSet =>
      routes += rs
  }

  override def receiveCommand: Receive = {
    case sr: SetRoute =>
      writeApplyAndSendItBack(RouteSet(sr.agentID, sr.routeInfo))

    case gr: GetRoute =>
      sender ! routes.find(_.agentID == gr.agentID)
  }
}