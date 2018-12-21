package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.core.msg_handler.{CoreAgentMsgHandler, DefaultRoutingAgent, RoutingAgent}
import com.evernym.agent.core.config.DefaultConfigProvider
import com.evernym.agent.core.Constants._
import com.evernym.agent.common.libindy.LedgerPoolConnManager
import com.evernym.agent.common.wallet.{LibIndyWalletProvider, WalletAPI, WalletConfig}
import com.evernym.agent.common.util.Util._
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

case class AgentActorCommonParam(commonParam: CommonParam, routingAgent: RoutingAgent,
                                 walletConfig: WalletConfig, walletAPI: WalletAPI)

object Main extends App {

  lazy val configProvider: ConfigProvider = DefaultConfigProvider
  lazy val system: ActorSystem = ActorSystem(AGENT_CORE_ACTOR_SYSTEM_NAME)
  lazy val materializer: Materializer = ActorMaterializer()(system)
  val poolConnManager: LedgerPoolConnManager = new LedgerPoolConnManager(configProvider)
  val walletAPI: WalletAPI = new WalletAPI(new LibIndyWalletProvider(configProvider), poolConnManager)
  val walletConfig: WalletConfig = buildWalletConfig(configProvider)

  implicit lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)
  val agentCommonParam: AgentActorCommonParam =
    AgentActorCommonParam(commonParam, new DefaultRoutingAgent, walletConfig, walletAPI)

  lazy val agentMsgHandler: AgentMsgHandler = new CoreAgentMsgHandler(agentCommonParam)


  lazy val defaultMsgRouter: TransportMsgRouter = new DefaultMsgRouter(configProvider, agentMsgHandler)


  lazy val akkaHttpTransportRouteParam: TransportHttpAkkaRouteParam =
    new DefaultTransportParamHttpAkka(commonParam, defaultMsgRouter)
  lazy val coreAgentTransport: Transport = new CoreAgentTransportAkkaHttp(commonParam, akkaHttpTransportRouteParam)
  //start transport
  coreAgentTransport.start()

}

