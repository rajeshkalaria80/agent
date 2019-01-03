package com.evernym.agent.core.platform.agent


import com.evernym.agent.api._
import com.evernym.agent.common.actor._
import com.evernym.agent.core.extension.DefaultExtensionManager
import com.evernym.agent.core.platform.business.CoreAgentBusinessPlatform
import com.evernym.agent.core.platform.transport.{CoreAgentTransportPlatform, DefaultTransportParamHttpAkka}

import scala.concurrent.Future


class CoreAgentPlatform(val commonParam: CommonParam) extends AgentPlatform with ActorRefResolver {

  lazy val extensionManager = new DefaultExtensionManager(commonParam.configProvider)
  var extensions: Set[Extension] = Set.empty
  var platforms: Set[Platform] = Set.empty
  var businessPlatform: BusinessPlatform = _

  override def handleMsg: PartialFunction[Any, Future[Any]] = {
    case tam: TransportMsg => businessPlatform.handleMsg(tam)
    case x => Future.failed(throw new NotImplementedError(s"messages not supported: $x"))
  }

  private def startCoreAgentBusinessPlatform(): Unit = {
    val akkaHttpRouteParam = new DefaultTransportParamHttpAkka(commonParam, handleMsg)
    val transportPlatform = new CoreAgentTransportPlatform(commonParam, akkaHttpRouteParam)
    transportPlatform.start()
    platforms = platforms ++ Set(transportPlatform)
  }

  private def startCoreAgentTransportPlatform(): Unit = {
    businessPlatform = new CoreAgentBusinessPlatform(commonParam)
    businessPlatform.start()
    platforms = platforms ++ Set(businessPlatform)
  }

  private def loadExpectedExtensions(): Unit = {
    extensionManager.startOptionalExtensionByName("extension-agency-business-platform",
      Option(commonParam)).foreach { aae =>
        extensions = extensions ++ Set(aae.extension)
        val transportExtensionParam = TransportExtensionParam(commonParam, aae.extension.handleMsg)
        extensionManager.startOptionalExtensionByName("extension-agency-transport-platform",
          Option(transportExtensionParam)).foreach { ate =>
            extensions = extensions ++ Set(ate.extension)
        }
    }
  }

  override def start(inputParam: Option[Any]=None): Unit = {
    extensionManager.load()
    startCoreAgentBusinessPlatform()
    startCoreAgentTransportPlatform()
    loadExpectedExtensions()
  }

  override def stop(): Unit = {
    extensions.toSeq.reverse.foreach(_.stop())
    platforms.toSeq.reverse.foreach(_.stop())
  }
}


