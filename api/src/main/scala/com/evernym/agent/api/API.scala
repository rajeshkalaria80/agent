package com.evernym.agent.api

import java.io.{File, FilenameFilter}

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


trait ExtensionParam

trait Extension extends MsgHandler {
  def config: ConfigProvider
  def name: String                                  //unique name of the extension
  def category: String                              //category (or type) of the extension
  def displayName: Option[String] = None
  final def getDisplayName: String = displayName.getOrElse(name)
  def apply(): Extension
}

trait TransportExtension extends Transport with Extension


trait ExtensionFilter extends FilenameFilter {
  def fileExtension: String

  override def accept(dir: File, name: String): Boolean = {
    name.endsWith(s"$fileExtension")
  }

}

trait ExtensionManager {

  def load(dirPaths: Set[String], filter: ExtensionFilter): Unit

  def getLoadedNames: Set[String]

  def getSupportedMsgTypes(name: String): Set[MsgType]

  def createExtension(name: String, param: Option[ExtensionParam] = None): Extension
}

