package com.evernym.agent.core_agent.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.core_agent.common._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait RoutingAgent {
  def setRoute(forId: String, routeJson: String): Unit
  def getRoute(forId: String): String
  def routeMsgToAgent(toId: String, msg: Msg): Future[Any]
}

class DefaultRoutingAgent(val config: Config) extends RoutingAgent {

  //it will be simple key value storage, may not be very efficient as it has to only support
  // one main agent actor and their pairwise actors info

  def setRoute(forId: String, routeJson: String): Unit = {

  }

  def getRoute(forId: String): String = {
    ""
  }

  def routeMsgToAgent(toId: String, msg: Msg): Future[Any] = {
    Future("")
  }
}

class DefaultAgentMsgHandler(val config: Config, val routingAgent: RoutingAgent) extends AgentMsgHandler {
  def handleMsg(msg: Msg): Future[Any] = {
    Future("")
  }
}


trait CorsSupport {

  def config: Config

  //this directive adds access control headers to normal responses
  private def addAccessControlHeaders(): Directive0 = {
    respondWithHeaders(
      //TODO: Insecure way of handling CORS, Consider securing it before moving to production
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Origin", "Authorization", "Accept", "Content-Type")
    )
  }

  //this handles preFlight OPTIONS requests.
  private def preFlightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).
      withHeaders(`Access-Control-Allow-Methods`(OPTIONS, HEAD, POST, PUT, GET, DELETE)))
  }

  def corsHandler(r: Route): Route = addAccessControlHeaders() {
    preFlightRequestHandler ~ r
  }
}


class DefaultTransportParamHttpAkka(val config: Config, val msgOrchestrator: MsgOrchestrator)
  extends TransportParamHttpAkka {

  class CoreAgentAkkaHttpRoute extends RouteDetail {
    override lazy val order: Int = 1

    override lazy val route: Route = logRequestResult("core-agent-service") {
      pathPrefix("agent") {
        path("msg") {
          post {
            complete("successful")
          }
        }
      }
    }
  }

  class TestAgentAkkaHttpRoute extends RouteDetail {
    override lazy val order: Int = 2

    override lazy val route: Route = logRequestResult("test-agent-service") {
      pathPrefix("test-agent") {
        path("test-msg") {
          post {
            complete("test-successful")
          }
        }
      }
    }
  }

  override lazy val routes: List[RouteDetail] = List (
    new CoreAgentAkkaHttpRoute, new TestAgentAkkaHttpRoute
  )
}

class DefaultTransportAkkaHttp (val param: TransportParamHttpAkka)
                               (implicit
                                val config: Config,
                                val actorSystem: ActorSystem,
                                val materializer: Materializer)
  extends TransportHttpAkka with CorsSupport {

  override def activate(): Unit = {
    val route: Route = param.routes.sortBy(_.order).map(_.route).reduce(_ ~ _)
    Http().bindAndHandle(corsHandler(route), "0.0.0.0", 6000)
  }

  override def deactivate(): Unit = {
    //
  }
}

class DefaultMsgOrchestrator(config: Config, val agentMsgHandler: AgentMsgHandler) extends MsgOrchestrator {

  def handleMsg(msg: Msg): Future[Any] = {
    Future("ok")
  }
}


object Main extends App {

  implicit lazy val config: Config = ConfigFactory.load()
  implicit lazy val system: ActorSystem = ActorSystem("agent")
  implicit lazy val materializer: Materializer = ActorMaterializer()

  lazy val agentMsgHandler: AgentMsgHandler = new DefaultAgentMsgHandler(config, new DefaultRoutingAgent(config))

  lazy val msgOrchestrator: MsgOrchestrator = new DefaultMsgOrchestrator(config, agentMsgHandler)

  implicit lazy val akkaHttpTransportParam: TransportParamHttpAkka =
    new DefaultTransportParamHttpAkka(config, msgOrchestrator)
  lazy val akkaHttpTransport: Transport = new DefaultTransportAkkaHttp(akkaHttpTransportParam)

  //start transport
  akkaHttpTransport.activate()

}

