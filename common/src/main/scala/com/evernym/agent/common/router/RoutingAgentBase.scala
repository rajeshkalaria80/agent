package com.evernym.agent.common.router

import akka.actor.ActorRef
import akka.pattern.ask
import com.evernym.agent.api.{CommonParam, RoutingAgent}
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.actor._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


trait RoutingAgentBase
  extends RoutingAgent
    with AgentJsonTransformationUtil
    with GeneralTimeout {

  def commonParam: CommonParam

  val ACTOR_PATH_PREFIX = "/user"

  val routingTable: ActorRef = commonParam.actorSystem.actorOf(
    KeyValueStore.props(commonParam.configProvider), "routing-table")

  def buildTargetActorRef(forId: String, routeDetail: RouteDetail): ActorRef

  def buildTargetMsg(forId: String, msg: Any): Any

  override def setRoute(forId: String, routeJson: String): Future[Either[Throwable, Any]] = {
    val futResp = routingTable ? StoreValue(forId, routeJson)
    futResp map {
      case vs: ValueStored => Right(vs)
      case x => Left(new RuntimeException(s"error while setting route: ${x.toString}"))
    }
  }

  override def getRoute(forId: String): Future[Either[Throwable, String]] = {
    val futResp = routingTable ? GetValue(forId)
    futResp map {
      case Some(routeJson: String) => Right(routeJson)
      case x => Left(new RuntimeException(s"error while getting route: ${x.toString}"))
    }
  }

  override def sendMsgToAgent(toId: String, msg: Any): Future[Either[Throwable, Any]] = {
    getRoute(toId).flatMap {
      case Right(routeJson: String) =>
        val routeDetail = convertJsonToNativeMsg[RouteDetail](routeJson)
        val actorRef = buildTargetActorRef(toId, routeDetail)
        val actorMsg = buildTargetMsg(toId, msg)
        val futResp = actorRef ? actorMsg
        futResp map {
          case r: Any => Right(r)
          case _ => Left(new RuntimeException(s"error while sending msg to route: $routeJson"))
        }
      case x => Future(Left(new RuntimeException(s"error while getting route: ${x.toString}")))
    }
  }
}


class BasicRoutingAgent(val commonParam: CommonParam)
  extends RoutingAgentBase
  with ActorRefResolver {

  override def buildTargetActorRef(forId: String, routeDetail: RouteDetail): ActorRef = {
    routeDetail.actorTypeId match {
      case ACTOR_TYPE_USER_AGENT_ACTOR | ACTOR_TYPE_USER_AGENT_PAIRWISE_ACTOR =>
        agentActorRefReq(forId, s"$ACTOR_PATH_PREFIX/$forId")
      case x => throw new NotImplementedError(s"path building not supported for actor type: $x")
    }
  }

  override def buildTargetMsg(forId: String, msg: Any): Any = {
    msg
  }
}