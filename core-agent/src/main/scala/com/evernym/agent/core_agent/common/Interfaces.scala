package com.evernym.agent.core_agent.common

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.Config

import scala.concurrent.Future


case class MsgInfoReq(ipAddress: String)
case class MsgInfoOpt(endpoint: Option[String]=None)
case class Msg(payload: Any, infoReq: MsgInfoReq, infoOpt: Option[MsgInfoOpt] = None)


trait Extension {
  def config: Config
  def name: String
  def groupName: String
  def displayName: Option[String] = None
  def getDisplayName: String = displayName.getOrElse(name)
}

trait MsgHandler {
  def handleMsg(msg: Msg): Future[Any]
}

trait AgentMsgHandler extends MsgHandler

trait MsgOrchestrator extends MsgHandler

trait TransportParam {
  def msgOrchestrator: MsgOrchestrator
}

trait Transport {
  def param: TransportParam
  def config: Config
  def activate(): Unit
  def deactivate(): Unit
}

trait TransportParamHttpAkka extends TransportParam {
  trait RouteDetail {
    def order: Int
    def route: Route
  }
  def routes: List[RouteDetail]

}

trait TransportHttpAkka extends Transport {
  override def param: TransportParamHttpAkka
  implicit def actorSystem: ActorSystem
  implicit def materializer: Materializer
}

