package com.evernym.agent.core.msg_handler


import akka.actor.{ActorRef, InvalidActorNameException}
import akka.pattern.ask
import com.evernym.agent.api.{AgentMsgHandler, CommonParam, RoutingAgent, TransportAgnosticMsg}
import com.evernym.agent.common.actor.AgentActorCommonParam
import com.evernym.agent.common.util.TransformationUtilBase
import com.evernym.agent.core.Constants._
import com.evernym.agent.core.actor.RouteSet
import com.evernym.agent.core.msg_handler.actor._
import com.evernym.agent.core.common.{ActorRefResolver, JsonTransformationUtil, RouteDetail}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class DefaultRoutingAgent(implicit val param: CommonParam)
  extends RoutingAgent
    with TransformationUtilBase
    with JsonTransformationUtil
    with ActorRefResolver {

  val routingAgent: ActorRef = param.actorSystem.actorOf(SimpleRoutingAgent.props(param.config))

  def getTargetActorRef(routeJson: String): ActorRef = {
    val routeDetail = convertJsonToNativeMsg[RouteDetail](routeJson)
    routeDetail.actorTypeId match {
      case ACTOR_TYPE_USER_AGENT_ACTOR => userAgent
      case ACTOR_TYPE_USER_AGENT_PAIRWISE_ACTOR => userAgentPairwise(routeDetail.persistenceId)
    }
  }

  def setRoute(forId: String, routeJson: String): Future[Either[Throwable, Any]] = {
    val futResp = routingAgent ? SetRoute(forId, routeJson)
    futResp map {
      case r: RouteSet => Right(r)
      case x => Left(new RuntimeException(s"error while setting route: ${x.toString}"))
    }
  }

  def getRoute(forId: String): Future[Either[Throwable, String]] = {
    val futResp = routingAgent ? GetRoute(forId)
    futResp map {
      case r: String => Right(r)
      case x => Left(new RuntimeException(s"error while getting route: ${x.toString}"))
    }
  }

  def routeMsgToAgent(toId: String, msg: Any): Future[Either[Throwable, Any]] = {
    getRoute(toId).flatMap {
      case Right(r: String) =>
        val actorRef = getTargetActorRef(r)
        val futResp = actorRef ? msg
        futResp map {
          case r: Any => Right(r)
          case _ => Left(new RuntimeException(s"error while sending msg to route: $r"))
        }
      case x => Future(Left(new RuntimeException(s"error while getting route: ${x.toString}")))
    }
  }
}


class CoreAgentMsgHandler(val agentCommonParam: AgentActorCommonParam)
  extends AgentMsgHandler with ActorRefResolver {

  implicit lazy val param: CommonParam = agentCommonParam.commonParam

  lazy val userAgentActorRef: ActorRef = {
    try {
      param.actorSystem.actorOf(UserAgent.props(agentCommonParam), USER_AGENT_ACTOR_NAME)
    } catch {
      case e: InvalidActorNameException =>
        val ar = param.actorSystem.child(USER_AGENT_ACTOR_NAME)
        userAgent
    }
  }

  def handleMsg(msg: Any): Future[Any] = {
    msg match {
      case tam: TransportAgnosticMsg => userAgentActorRef ? tam.payload
    }
  }
}


