## High level design

<pre>
///////////////////////////////////////////////////////////////////////////////////


  ////////////////////                                      ////////////////////
  //                //                                      //                //
  //   TRANSPORT1   //                                      //   Core-Agent   //
  //    (http)      //                                      //      MSG       //
  //                //                                      //    HANDLER     //
  //                //                                      //                //
  ////////////////////                                      ////////////////////


                              ////////////////////          ////////////////////
                              //                //          //                //
                              //                //          //   Extension1   //
                              //     CORE       //          //     MSG        //
                              //     AGENT      //          //    HANDLER     //
                              //                //          //                //
                              ////////////////////          ////////////////////


  ////////////////////                                      ////////////////////
  //                //                                      //                //
  //   TRANSPORT2   //                                      //   Extension2   //
  //      (mq)      //                                      //      MSG       //
  //                //                                      //    HANDLER     //
  //                //                                      //                //
  ////////////////////                                      ////////////////////


///////////////////////////////////////////////////////////////////////////////////

</pre>


## open questions regarding supporting extension
  * what will be the input from agent to extensions?
    * **actor system:** extension may wanna create required actors under that actor system so either core-agent 
      can supply its actor system to the extension or extension will have to create its own.
      * if the actor system is created by agent and provided to extension, then that actor system's configuration 
      would have been created by combining all default configurations from agent's dependencies 
      (like akka-actor, akka-persistence etc), now the problem is if that extension try to create any actor 
      (for example cluster sharding region actor) which depends on extension's dependency, it doesn't find required 
      default configuration from that provided actor system's configuration.
        * even if this mechanism works, we'll have to understand security risk of sending actor system object to extension.
      * Or if extension creates new actor system for their need, eventually there may be more than one actor system running 
      in one agent process, we'll have to do some reading about its impact:
        * https://manuel.bernhardt.io/2016/08/23/akka-anti-patterns-too-many-actor-systems
      * Or what if that component is not an extension installed on top of core agent, rather that component (imagine agency) 
      is stand alone application who depends on core-agent.  
    * **configuration:** assuming extensions will have default configuration (either part of the extension 
      library config or hardcoded), but there should be a way for extension installer to override those configurations.
      
      
## useful commands   
### build api and publish to local ivy repository so that plugin can use it
sbt assembly; sbt publish-local
  
