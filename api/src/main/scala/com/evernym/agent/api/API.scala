package com.evernym.agent.api

import java.util.jar.Manifest

import scala.concurrent.Future


case class MsgInfoReq(ipAddress: String)
case class MsgInfoOpt(endpoint: Option[String]=None)
case class Msg(payload: Any, infoReq: MsgInfoReq, infoOpt: Option[MsgInfoOpt] = None)


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
  def config: ConfigProvider
  def activate(): Unit
  def deactivate(): Unit
}

case class MsgType(name: String, version: String)


trait Extension extends MsgHandler {
  def config: ConfigProvider
  def name: String                                  //unique name of the extension
  def category: String                              //category (or type) of the extension
  def displayName: Option[String] = None
  final def getDisplayName: String = displayName.getOrElse(name)

}

trait TransportExtension extends Transport with Extension


trait ExtensionParam


trait ExtensionWrapper {
  def manifest: Manifest
  def getSupportedMsgTypes: Set[MsgType]

  def create(param: ExtensionParam): Extension
}


trait ExtensionManager {

  def load(dirPaths: Set[String]): Map[String, ExtensionWrapper]
  def getLoaded: Map[String, ExtensionWrapper]

  def activate(name: String, param: ExtensionParam): Unit
  def deactivate(name: String): Unit

  def getActivatedExtension: Map[String, Extension]

}

