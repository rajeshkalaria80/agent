package com.evernym.agent.common.actor

import akka.actor.Props
import com.evernym.agent.api.ConfigProvider


//cmd
case class SetRoute(agentId: String, routeInfo: String)
case class GetRoute(agentId: String)


object RoutingTable {
  def props(configProvider: ConfigProvider) = Props(new RoutingTable(configProvider))
}


class RoutingTable(configProvider: ConfigProvider) extends PersistentActorBase {

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
