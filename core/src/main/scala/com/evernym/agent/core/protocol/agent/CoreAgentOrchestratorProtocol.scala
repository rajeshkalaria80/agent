package com.evernym.agent.core.protocol.agent


import com.evernym.agent.api._
import com.evernym.agent.common.actor._
import com.evernym.agent.core.extension.DefaultExtensionManager
import com.evernym.agent.core.protocol.business.CoreAgentBusinessProtocol
import com.evernym.agent.core.protocol.transport.{CoreAgentTransportProtocol, DefaultTransportParamHttpAkka}

import scala.concurrent.Future


class CoreAgentOrchestratorProtocol(val commonParam: CommonParam) extends AgentOrchestratorProtocol with ActorRefResolver {

  lazy val extensionManager = new DefaultExtensionManager(commonParam.configProvider)
  var extensions: Set[Extension] = Set.empty
  var protocols: Set[Protocol] = Set.empty
  var coreAgentBusinessProtocol: BusinessProtocol = _

  private def startCoreAgentBusinessProtocol(): Unit = {
    val akkaHttpRouteParam = new DefaultTransportParamHttpAkka(commonParam, handleMsg)
    val transportProtocol = new CoreAgentTransportProtocol(commonParam, akkaHttpRouteParam)
    transportProtocol.start()
    protocols = protocols ++ Set(transportProtocol)
  }

  private def startCoreAgentTransportProtocol(): Unit = {
    coreAgentBusinessProtocol = new CoreAgentBusinessProtocol(commonParam)
    coreAgentBusinessProtocol.start()
    protocols = protocols ++ Set(coreAgentBusinessProtocol)
  }

  private def loadExpectedExtensions(): Unit = {
    extensionManager.startOptionalExtensionByName("extension-agency-business-protocol",
      Option(commonParam)).foreach { aae =>
        extensions = extensions ++ Set(aae.extension)
        val transportExtensionParam = TransportExtensionParam(commonParam, aae.extension.handleMsg)
        extensionManager.startOptionalExtensionByName("extension-agency-transport-protocol",
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
    startCoreAgentBusinessProtocol()
    startCoreAgentTransportProtocol()
    loadExpectedExtensions()
  }

  override def stop(): Unit = {
    extensions.toSeq.reverse.foreach(_.stop())
    protocols.toSeq.reverse.foreach(_.stop())
  }
}


