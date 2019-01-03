package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.config.DefaultConfigProvider
import Constants._
import com.evernym.agent.core.platform.agent.CoreAgentPlatform


//////////////////////////////////////////////////////////////////////////////////


  ////////////////////                                     ////////////////////
  //                //                                     //                //
  //   TRANSPORT1   //                                     //     AGENT      //
  //    (http)      //                                     //      MSG       //
  //                //                                     //    HANDLER     //
  //                //                                     //                //
  ////////////////////                                     ////////////////////


                              ////////////////////         ////////////////////
                              //                //         //                //
                              //      MSG       //         //    AGENCY      //
                              //  ORCHESTRATOR  //         //     MSG        //
                              //                //         //    HANDLER     //
                              //                //         //                //
                              ////////////////////         ////////////////////


  ////////////////////                                     ////////////////////
  //                //                                     //                //
  //   TRANSPORT2   //                                     //    PAYMENT     //
  //      (mq)      //                                     //      MSG       //
  //                //                                     //    HANDLER     //
  //                //                                     //                //
  ////////////////////                                     ////////////////////


//////////////////////////////////////////////////////////////////////////////////



object Main extends App {

  lazy val configProvider: ConfigProvider = DefaultConfigProvider
  lazy val system: ActorSystem = ActorSystem(CORE_AGENT_ACTOR_SYSTEM_NAME)
  lazy val materializer: Materializer = ActorMaterializer()(system)

  lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)

  lazy val coreAgentPlatform = new CoreAgentPlatform(commonParam)
  coreAgentPlatform.start()

}

