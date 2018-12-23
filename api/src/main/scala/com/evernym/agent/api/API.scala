package com.evernym.agent.api

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

import scala.concurrent.Future


trait ConfigProvider {

  def getStringSet(path: String): Set[String]

  def getConfigIntReq(key: String): Int

  def getConfigIntOption(key: String): Option[Int]

  def getConfigStringReq(key: String): String

  def getConfigStringOption(key: String): Option[String]
}

//any "required" detail about message
// 'ipAddress' is just an example, we may decide it is not required
case class MsgInfoReq(ipAddress: String)

//any "optional" detail about message
//'endpoint' might not be good example (as it is transport dependent), need to replace it with some good candidate
case class MsgInfoOpt(endpoint: Option[String] = None)

//TODO: temporarily named it with word "TransportAgnostic", later on we may remove it
case class TransportAgnosticMsg(payload: Any, infoReq: Option[MsgInfoReq] = None, infoOpt: Option[MsgInfoOpt] = None)


trait RoutingAgent {
  def setRoute(forId: String, routeJson: String): Future[Either[Throwable, String]]
  def getRoute(forId: String): Future[Either[Throwable, String]]
  def routeMsgToAgent(toId: String, msg: Any): Future[Either[Throwable, Any]]
}


trait MsgHandler {
  def handleMsg(msg: Any): Future[Any]
}

trait AgentMsgHandler extends MsgHandler

trait TransportMsgRouter {
  def handleMsg(msg: TransportAgnosticMsg): Future[Any]
}

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

case class CommonParam (config: ConfigProvider,
                         actorSystem: ActorSystem,
                         materializer: Materializer)


trait TransportHttpAkkaRouteParam {
  def route: Route
}

trait ExtFileFilterCriteria {
  def filteredFiles(folderPath: String): Set[File]
}

case class ExtensionDetail(extension: Extension, fileAbsolutePath: String)
case class StatusDetail(code: Int, message: Option[String]=None)


trait ExtensionManager {

  //this load method can be called as many times as you want with different inputs,
  // it should just keep loading extensions if not already loaded
  def load(dirPathsOpt: Option[Set[String]] = None, filterOpt: Option[ExtFileFilterCriteria] = None): Unit

  def getSuccessfullyLoaded: Set[ExtensionDetail]

  def getErrors: Set[StatusDetail]

  final def getExtOption(name: String): Option[ExtensionDetail] =
    getSuccessfullyLoaded.find(_.extension.name == name)

  final def getExtReq(name: String): ExtensionDetail = getExtOption(name).
    getOrElse(throw new RuntimeException(s"extension with name '$name' not found in successfully loaded extensions"))

}

