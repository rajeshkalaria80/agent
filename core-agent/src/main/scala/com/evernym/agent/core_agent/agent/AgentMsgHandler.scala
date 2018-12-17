package com.evernym.agent.core_agent.agent


import com.evernym.agent.api.{AgentMsgHandler, CommonParam, Msg}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait RoutingAgent {
  def setRoute(forId: String, routeJson: String): Future[String]
  def getRoute(forId: String): Future[String]
  def routeMsgToAgent(toId: String, msg: Msg): Future[Any]
}

class DefaultRoutingAgent(val param: CommonParam) extends RoutingAgent {

  //it will be simple key value storage, may not be very efficient as it has to only support
  // one main agent actor and their pairwise actors info

  def setRoute(forId: String, routeJson: String): Future[String] = {
    Future("done")
  }

  def getRoute(forId: String): Future[String] = {
    Future("json")
  }

  def routeMsgToAgent(toId: String, msg: Msg): Future[Any] = {
    Future("sent")
  }
}

class DefaultAgentMsgHandler(val param: CommonParam, val routingAgent: RoutingAgent) extends AgentMsgHandler {
  def handleMsg(msg: Msg): Future[Any] = {
    Future("")
  }
}


