package com.evernym.agent.core.builder

import com.evernym.agent.api._
import com.evernym.agent.common.a2a.{AgentToAgentAPI, DefaultAgentToAgentAPI}
import com.evernym.agent.common.actor.AgentActorCommonParam
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.util.Util.createWalletConfig
import com.evernym.agent.common.wallet._
import com.evernym.agent.core.extension.DefaultExtensionManager
import com.evernym.agent.core.msg_handler.{CoreAgentMsgHandler, DefaultRoutingAgent}
import com.evernym.agent.core.router.DefaultMsgOrchestrator
import com.evernym.agent.core.transport.http.akka.{CoreAgentTransportAkkaHttp, DefaultTransportParamHttpAkka}


class DefaultWalletAPI(val walletProvider: WalletProvider, val ledgerPoolManager: LedgerPoolConnManager) extends WalletAPI


trait AgentApp {

  protected def buildExtensionManager(): ExtensionManager
  protected def buildMsgHandlers(): Set[MsgHandler]
  protected def buildMsgOrchestrator(msgHandlers: Set[MsgHandler]): MsgOrchestrator
  protected def buildTransports(extensionManager: ExtensionManager, transportMsgRouter: MsgOrchestrator): Set[Transport]

  lazy val extensionManager: ExtensionManager = buildExtensionManager()
  lazy val msgHandlers: Set[MsgHandler] = buildMsgHandlers()
  lazy val msgOrchestrator: MsgOrchestrator = buildMsgOrchestrator(msgHandlers)
  lazy val transports: Set[Transport] = buildTransports(extensionManager, msgOrchestrator)

  def run(): Unit = transports.foreach(_.start())
}

class CoreAgentApp(val commonParam: CommonParam) extends AgentApp {
  val configProvider: ConfigProvider = commonParam.configProvider

  override def buildExtensionManager(): ExtensionManager = {
    new DefaultExtensionManager(configProvider)
  }

  override def buildMsgHandlers(): Set[MsgHandler] = {
    val poolConnManager: LedgerPoolConnManager = new LedgerPoolConnManager(configProvider)
    val walletAPI: WalletAPI = new DefaultWalletAPI(new LibIndyWalletProvider(configProvider), poolConnManager)
    val walletConfig: WalletConfig = createWalletConfig(configProvider)
    val agentToAgentAPI: AgentToAgentAPI = new DefaultAgentToAgentAPI(walletAPI)

    val agentActorCommonParam: AgentActorCommonParam =
    AgentActorCommonParam(commonParam, new DefaultRoutingAgent(commonParam), walletConfig, walletAPI, agentToAgentAPI)

    Set(new CoreAgentMsgHandler(agentActorCommonParam))
  }

  override def buildMsgOrchestrator(msgHandlers: Set[MsgHandler]): MsgOrchestrator = {
    new DefaultMsgOrchestrator(configProvider, msgHandlers)
  }

  override def buildTransports(extensionManager: ExtensionManager, transportMsgRouter: MsgOrchestrator): Set[Transport] = {
    val akkaHttpRouteParam = new DefaultTransportParamHttpAkka(commonParam, transportMsgRouter)
    val defaultTransport = new CoreAgentTransportAkkaHttp(commonParam, akkaHttpRouteParam)
    Set(defaultTransport)
  }

}