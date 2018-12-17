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

trait Extension extends MsgHandler {
    def name: String
    def category: String
    def displayName: Option[String] = None
    final def getDisplayName: String = displayName.getOrElse(name)
}

case class MsgType(name: String, version: String)

trait ExtensionWrapper extends MsgHandler {
  def name: String
  def category: String
  def createExtension(inputParam: Option[Any] = None): Unit
}

case class CommonParam (config: ConfigProvider, actorSystem: ActorSystem, materializer: Materializer)

trait TransportHttpAkkaRouteParam {
  trait RouteDetail {
    def order: Int
    def route: Route
  }
  def routes: List[RouteDetail]
}

trait TransportExtension extends Transport with Extension

trait AkkaHttpTransportExtension extends TransportExtension {
  def commonParam: CommonParam
}


trait ExtensionFilter extends FilenameFilter {
  def fileExtension: String

  override def accept(dir: File, name: String): Boolean = {
    name.endsWith(s"$fileExtension")
  }
}


trait ExtensionManager {

  def load(dirPaths: Set[String], filter: ExtensionFilter): Unit

  def getExtWrappers: Map[String, ExtensionWrapper]

  def getExtWrapperOption(name: String): Option[ExtensionWrapper] = getExtWrappers.get(name)

  def getExtWrapperReq(name: String): ExtensionWrapper = getExtWrapperOption(name).
    getOrElse(throw new RuntimeException(s"extension with name $name not found"))

}

