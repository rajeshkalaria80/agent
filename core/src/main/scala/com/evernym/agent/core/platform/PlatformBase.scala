package com.evernym.agent.core.platform

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.evernym.agent.api._
import com.evernym.agent.common.a2a.{AgentToAgentAPI, DefaultAgentToAgentAPI}
import com.evernym.agent.common.actor.AgentActorCommonParam
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.util.Util.buildWalletConfig
import com.evernym.agent.common.wallet.{LibIndyWalletProvider, WalletAPI, WalletConfig}
import com.evernym.agent.core.msg_handler.{CoreAgentMsgHandler, DefaultRoutingAgent}
import com.evernym.agent.core.router.DefaultMsgRouter
import com.evernym.agent.core.transport.http.akka.{CoreAgentTransportAkkaHttp, DefaultTransportParamHttpAkka}

trait PlatformBase {

  implicit def commonParam: CommonParam

  lazy val configProvider: ConfigProvider = commonParam.configProvider
  lazy val actorSystem: ActorSystem = commonParam.actorSystem
  lazy val materializer: Materializer = commonParam.materializer

  lazy val poolConnManager: LedgerPoolConnManager = new LedgerPoolConnManager(configProvider)
  lazy val walletAPI: WalletAPI = new WalletAPI(new LibIndyWalletProvider(configProvider), poolConnManager)
  lazy val walletConfig: WalletConfig = buildWalletConfig(configProvider)
  lazy val defaultA2AAPI: AgentToAgentAPI = new DefaultAgentToAgentAPI(walletAPI)

  lazy val agentCommonParam: AgentActorCommonParam =
    AgentActorCommonParam(commonParam, new DefaultRoutingAgent, walletConfig, walletAPI, defaultA2AAPI)

  lazy val agentMsgHandler: AgentMsgHandler = new CoreAgentMsgHandler(agentCommonParam)

  lazy val defaultMsgRouter: TransportMsgRouter = new DefaultMsgRouter(configProvider, agentMsgHandler)

  lazy val akkaHttpTransportRouteParam: TransportHttpAkkaRouteParam =
    new DefaultTransportParamHttpAkka(commonParam, defaultMsgRouter)
  lazy val transport: Transport = new CoreAgentTransportAkkaHttp(commonParam, akkaHttpTransportRouteParam)

  transport.start()
}