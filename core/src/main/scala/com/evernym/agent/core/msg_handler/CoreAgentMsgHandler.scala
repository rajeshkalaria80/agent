package com.evernym.agent.core.msg_handler


import akka.actor.ActorRef
import akka.pattern.ask
import com.evernym.agent.api.{AgentMsgHandler, CommonParam, RoutingAgent, TransportAgnosticMsg}
import com.evernym.agent.common.actor._
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.core.common.Constants._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class DefaultRoutingAgent(implicit val param: CommonParam)
  extends RoutingAgent
    with AgentJsonTransformationUtil
    with ActorRefResolver {

  val ACTOR_PATH_PREFIX = "/user"

  val routingAgent: ActorRef = param.actorSystem.actorOf(SimpleRoutingAgent.props(param.configProvider))

  def getTargetActorRef(agentId: String, routeJson: String): ActorRef = {
    val routeDetail = convertJsonToNativeMsg[RouteDetail](routeJson)
    routeDetail.actorTypeId match {
      case ACTOR_TYPE_USER_AGENT_ACTOR | ACTOR_TYPE_USER_AGENT_PAIRWISE_ACTOR =>
        agentActorRefReq(agentId, s"$ACTOR_PATH_PREFIX/$agentId")
    }
  }

  override def setRoute(forId: String, routeJson: String): Future[Either[Throwable, Any]] = {
    val futResp = routingAgent ? SetRoute(forId, routeJson)
    futResp map {
      case r: RouteSet => Right(r)
      case x => Left(new RuntimeException(s"error while setting route: ${x.toString}"))
    }
  }

  override def getRoute(forId: String): Future[Either[Throwable, String]] = {
    val futResp = routingAgent ? GetRoute(forId)
    futResp map {
      case Some(routeJson: String) => Right(routeJson)
      case x => Left(new RuntimeException(s"error while getting route: ${x.toString}"))
    }
  }

  override def sendMsgToAgent(toId: String, msg: Any): Future[Either[Throwable, Any]] = {
    getRoute(toId).flatMap {
      case Right(routeJson: String) =>
        val actorRef = getTargetActorRef(toId, routeJson)
        val futResp = actorRef ? msg
        futResp map {
          case r: Any => Right(r)
          case _ => Left(new RuntimeException(s"error while sending msg to route: $routeJson"))
        }
      case x => Future(Left(new RuntimeException(s"error while getting route: ${x.toString}")))
    }
  }
}


class CoreAgentMsgHandler(val agentCommonParam: AgentActorCommonParam)
  extends AgentMsgHandler with ActorRefResolver {

  implicit lazy val param: CommonParam = agentCommonParam.commonParam

  def userAgentActorRefOpt: Option[ActorRef] = agentActorRefOpt(USER_AGENT_ID, s"$USER_AGENT_ID/$USER_AGENT_ID")

  lazy val userAgentActorRef: ActorRef = userAgentActorRefOpt.getOrElse {
    param.actorSystem.actorOf(UserAgent.props(agentCommonParam), USER_AGENT_ID)
  }

  def handleMsg(msg: Any): Future[Any] = {
    msg match {
      case tam: TransportAgnosticMsg => userAgentActorRef ? tam.payload
    }
  }
}


