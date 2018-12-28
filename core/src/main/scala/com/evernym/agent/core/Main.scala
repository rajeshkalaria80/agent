package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.config.DefaultConfigProvider
import com.evernym.agent.core.common.Constants._
import com.evernym.agent.core.platform.PlatformBase



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


class Platform (implicit val commonParam: CommonParam) extends PlatformBase

object Main extends App {

  lazy val configProvider: ConfigProvider = DefaultConfigProvider
  implicit lazy val system: ActorSystem = ActorSystem(CORE_AGENT_ACTOR_SYSTEM_NAME)
  lazy val materializer: Materializer = ActorMaterializer()

  implicit lazy val commonParam: CommonParam = CommonParam(configProvider, system, materializer)

  val platform = new Platform
}

