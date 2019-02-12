package com.evernym.agent.core.protocol.agent


import com.evernym.agent.api._
import com.evernym.agent.common.extension.DefaultExtensionManager
import com.evernym.agent.core.protocol.business.CoreAgentBusinessProtocol
import com.evernym.agent.core.protocol.transport.{CoreAgentTransportProtocol, DefaultTransportParamHttpAkka}
import com.evernym.agent.core.Constants._

import scala.concurrent.Future


class CoreAgentOrchestrator(val commonParam: CommonParam) extends AgentOrchestrator {

  lazy val extensionManager = new DefaultExtensionManager(commonParam.configProvider)
  var extensions: Set[Extension] = Set.empty
  var protocols: Set[Protocol] = Set.empty
  var coreAgentBusinessProtocol: BusinessProtocol = _

  private def startCoreAgentTransportProtocol(): Unit = {
    val akkaHttpRouteParam = new DefaultTransportParamHttpAkka(commonParam, handleMsg)
    val transportProtocol = new CoreAgentTransportProtocol(commonParam, akkaHttpRouteParam)
    transportProtocol.start()
    protocols = protocols ++ Set(transportProtocol)
  }

  private def startCoreAgentBusinessProtocol(): Unit = {
    coreAgentBusinessProtocol = new CoreAgentBusinessProtocol(commonParam)
    coreAgentBusinessProtocol.start()
    protocols = protocols ++ Set(coreAgentBusinessProtocol)
  }

  private def loadExpectedExtensions(): Unit = {
    extensionManager.startOptionalExtensionByName(PROTOCOL_EXTENSION_AGENCY_BUSINESS,
      Option(commonParam)).foreach { aae =>
      extensions = extensions ++ Set(aae.extension)
      val transportExtensionParam = TransportProtocolExtensionParam(commonParam, aae.extension.handleMsg)
      extensionManager.startOptionalExtensionByName(PROTOCOL_EXTENSION_AGENCY_TRANSPORT,
        Option(transportExtensionParam)).foreach { ate =>
        extensions = extensions ++ Set(ate.extension)
      }
    }
  }

  override def handleMsg: PartialFunction[Any, Future[Any]] = {
    case tam: TransportMsg => coreAgentBusinessProtocol.handleMsg(tam)
    case x => Future.failed(throw new NotImplementedError(s"messages not supported: $x"))
  }

  override def start(inputParam: Option[Any]=None): Unit = {
    extensionManager.load()
    startCoreAgentTransportProtocol()
    startCoreAgentBusinessProtocol()
    loadExpectedExtensions()
  }

  override def stop(): Unit = {
    extensions.toSeq.reverse.foreach(_.stop())
    protocols.toSeq.reverse.foreach(_.stop())
  }

}


