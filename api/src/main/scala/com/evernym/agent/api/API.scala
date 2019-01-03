package com.evernym.agent.api

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ConfigProvider {

  def getStringSet(path: String): Set[String]

  def getConfigIntReq(key: String): Int

  def getConfigIntOption(key: String): Option[Int]

  def getConfigStringReq(key: String): String

  def getConfigStringOption(key: String): Option[String]

  def getConfigOption(key: String): Option[Config]
}

case class MsgInfoOpt(ipAddress: String, transportName: Option[String] = None)


//TODO: Here 'TransportMsg' means msg with some transport related information
case class TransportMsg(senderName: String, genericMsg: GenericMsg)

//TODO: Here 'GenericMsg' means msg without any transport specific information, if required rename it appropriately
case class GenericMsg(payload: Any, infoOpt: Option[MsgInfoOpt] = None)


trait RoutingAgent {

  def setRoute(forId: String, routeJson: String): Future[Either[Throwable, Any]]

  def getRoute(forId: String): Future[Either[Throwable, String]]

  def sendMsgToAgent(toId: String, msg: Any): Future[Either[Throwable, Any]]

  final def fwdMsgToAgent(toId: String, msg: Any, sender: ActorRef): Unit = {
    val sndr = sender
    sendMsgToAgent(toId, msg).map {
      case Right(r: Any) => sndr ! r
      case Left(e: Throwable) => throw e
    }
  }
}


trait MsgHandler {
  def handleMsg: PartialFunction[Any, Future[Any]]
}

trait Platform extends MsgHandler {

  def start(inputParam: Option[Any]=None): Unit

  def stop(): Unit

  override def handleMsg: PartialFunction[Any, Future[Any]] = {
    case m => Future.failed(throw new RuntimeException("not supported"))
  }
}

trait BusinessPlatform extends Platform {
  override def stop(): Unit = {}
}

trait TransportPlatform extends Platform

trait AgentPlatform extends Platform


case class MsgType(name: String, version: String)

trait Extension extends MsgHandler {
  def name: String
  def category: String
  def getSupportedMsgTypes: Set[MsgType]
  def start(inputParam: Option[Any]=None): Unit
  def stop(): Unit
}

case class CommonParam (configProvider: ConfigProvider, actorSystem: ActorSystem, materializer: Materializer)

case class TransportExtensionParam (commonParam: CommonParam, msgHandler: PartialFunction[Any, Future[Any]])

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

