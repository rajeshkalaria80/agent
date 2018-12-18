package com.evernym.agent.api

import java.io.{File, FilenameFilter}

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.Future


case class MsgInfoReq(ipAddress: Option[String] = None)
case class MsgInfoOpt(endpoint: Option[String] = None)
case class Msg(payload: Any, infoReq: MsgInfoReq, infoOpt: Option[MsgInfoOpt] = None)


trait MsgHandler {
  def handleMsg(msg: Msg): Future[Any]
}

trait AgentMsgHandler extends MsgHandler

trait MsgOrchestrator extends MsgHandler

trait Transport {
  def start(): Unit
  def stop(): Unit
}

case class MsgType(name: String, version: String)

trait Extension extends MsgHandler {
  def name: String
  def category: String
  def getSupportedMsgTypes: Set[MsgType]
  def init(inputParam: Option[Any]): Unit
}

case class CommonParam (config: ConfigProvider, actorSystem: ActorSystem, materializer: Materializer)

trait TransportHttpAkkaRouteParam {
  trait RouteDetail {
    def order: Int
    def route: Route
  }
  def routes: List[RouteDetail]
}

trait ExtensionFilter extends FilenameFilter {
  def fileExtension: String

  override def accept(dir: File, name: String): Boolean = {
    name.endsWith(s"$fileExtension")
  }
}


trait ExtensionManager {

  def load(dirPathsOpt: Option[Set[String]] = None, filterOpt: Option[ExtensionFilter] = None): Unit

  def getExtensions: Map[String, Extension]

  def getExtOption(name: String): Option[Extension] = getExtensions.get(name)

  def getExtReq(name: String): Extension = getExtOption(name).
    getOrElse(throw new RuntimeException(s"extension with name $name not found"))

}

