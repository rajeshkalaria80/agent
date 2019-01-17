package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.config.DefaultConfigProvider
import Constants._


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
                              //                //         //    AGENCY      //
                              //     CORE       //         //     MSG        //
                              //     AGENT      //         //    HANDLER     //
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
  lazy val actorSystem: ActorSystem = ActorSystem(CORE_AGENT_ACTOR_SYSTEM_NAME, configProvider.getConfig)
  lazy val materializer: Materializer = ActorMaterializer()(actorSystem)

  lazy val commonParam: CommonParam = CommonParam(configProvider, actorSystem, materializer)

  lazy val coreAgentApp = new CoreAgent(commonParam)
  coreAgentApp.start()

}

