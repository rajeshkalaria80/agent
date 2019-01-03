package com.evernym.agent.core.platform.business

import akka.actor.ActorRef
import akka.pattern.ask
import com.evernym.agent.api._
import com.evernym.agent.common.a2a.{AgentToAgentAPI, DefaultAgentToAgentAPI}
import com.evernym.agent.common.actor._
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.router.BasicRoutingAgent
import com.evernym.agent.common.util.Util.createWalletConfig
import com.evernym.agent.common.wallet.{DefaultWalletAPI, LibIndyWalletProvider, WalletAPI, WalletConfig}
import com.evernym.agent.core.Constants._

import scala.concurrent.Future


class CoreAgentBusinessPlatform(val commonParam: CommonParam) extends BusinessPlatform with ActorRefResolver {

  def userAgentActorRefOpt: Option[ActorRef] = agentActorRefOpt(USER_AGENT_ID, s"$USER_AGENT_ID/$USER_AGENT_ID")

  lazy val userAgentActorRef: ActorRef = userAgentActorRefOpt.getOrElse {
    val walletAPI: WalletAPI = new DefaultWalletAPI(
      new LibIndyWalletProvider(commonParam.configProvider), new LedgerPoolConnManager(commonParam.configProvider))
    val walletConfig: WalletConfig = createWalletConfig(commonParam.configProvider)
    val agentToAgentAPI: AgentToAgentAPI = new DefaultAgentToAgentAPI(walletAPI)
    val agentActorCommonParam: AgentActorCommonParam =
      AgentActorCommonParam(commonParam, new BasicRoutingAgent(commonParam), walletConfig, walletAPI, agentToAgentAPI)
    commonParam.actorSystem.actorOf(UserAgent.props(agentActorCommonParam), USER_AGENT_ID)
  }

  override def handleMsg: PartialFunction[Any, Future[Any]] = {
    case tam: TransportMsg => userAgentActorRef ? tam.genericMsg.payload
    case x => Future.failed(throw new NotImplementedError(s"messages not supported: $x"))
  }

  override def start(inputParam: Option[Any]=None): Unit = {
    userAgentActorRef
  }
}


