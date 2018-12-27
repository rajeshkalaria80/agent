package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.a2a.{AgentToAgentAPI, DefaultAgentToAgentAPI}
import com.evernym.agent.common.actor.AgentActorCommonParam
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.wallet.{LibIndyWalletProvider, WalletAPI, WalletConfig}
import com.evernym.agent.common.util.Util._
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.core.msg_handler.{CoreAgentMsgHandler, DefaultRoutingAgent}
import com.evernym.agent.core.config.DefaultConfigProvider
import com.evernym.agent.core.router.DefaultMsgRouter
import com.evernym.agent.core.transport.http.akka._



/////////////////////////////////////////////////////////////////////////////////


  ////////////////////                                    ////////////////////
  //                //                                    //                //
  //   TRANSPORT1   //                                    //     AGENT      //
  //    (http)      //                                    //      MSG       //
  //                //                                    //    HANDLER     //
  //                //                                    //                //
  ////////////////////                                    ////////////////////


                              ////////////////////        ////////////////////
                              //                //        //                //
                              //    TRANSPORT   //        //    AGENCY      //
                              //       MSG      //        //     MSG        //
                              //     ROUTER     //        //    HANDLER     //
                              //                //        //                //
                              ////////////////////        ////////////////////


  ////////////////////                                    ////////////////////
  //                //                                    //                //
  //   TRANSPORT2   //                                    //    PAYMENT     //
  //      (mq)      //                                    //      MSG       //
  //                //                                    //    HANDLER     //
  //                //                                    //                //
  ////////////////////                                    ////////////////////


/////////////////////////////////////////////////////////////////////////////////


trait Platform {
  def configProvider: ConfigProvider
  def system: ActorSystem
  def materializer: Materializer

  val poolConnManager: LedgerPoolConnManager = new LedgerPoolConnManager(configProvider)
  val walletAPI: WalletAPI = new WalletAPI(new LibIndyWalletProvider(configProvider), poolConnManager)
  val walletConfig: WalletConfig = buildWalletConfig(configProvider)
  val defaultA2AAPI: AgentToAgentAPI = new DefaultAgentToAgentAPI(walletAPI)

  implicit lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)
  lazy val agentCommonParam: AgentActorCommonParam =
    AgentActorCommonParam(commonParam, new DefaultRoutingAgent, walletConfig, walletAPI, defaultA2AAPI)

  lazy val agentMsgHandler: AgentMsgHandler = new CoreAgentMsgHandler(agentCommonParam)

  lazy val defaultMsgRouter: TransportMsgRouter = new DefaultMsgRouter(configProvider, agentMsgHandler)

  lazy val akkaHttpTransportRouteParam: TransportHttpAkkaRouteParam =
    new DefaultTransportParamHttpAkka(commonParam, defaultMsgRouter)
  lazy val coreAgentTransport: Transport = new CoreAgentTransportAkkaHttp(commonParam, akkaHttpTransportRouteParam)
  //start transport
  coreAgentTransport.start()
}


object Main extends App with Platform {
  lazy val configProvider: ConfigProvider = DefaultConfigProvider
  implicit lazy val system: ActorSystem = ActorSystem(AGENT_CORE_ACTOR_SYSTEM_NAME)
  implicit lazy val materializer: Materializer = ActorMaterializer()
}

