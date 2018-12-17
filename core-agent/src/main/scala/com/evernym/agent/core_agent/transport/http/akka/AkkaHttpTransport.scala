package com.evernym.agent.core_agent.transport.http.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives.{complete, logRequestResult, options, path, pathPrefix, post}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import akka.stream.Materializer
import com.evernym.agent.api._


trait CorsSupport {

  def config: ConfigProvider

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

class DefaultTransportParamHttpAkka(val config: ConfigProvider, val msgOrchestrator: MsgOrchestrator)
  extends TransportHttpAkkaRouteParam {

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


class DefaultTransportAkkaHttp(val commonParam: CommonParam, val routeParam: TransportHttpAkkaRouteParam)
  extends Transport with CorsSupport {

  implicit val name: String = "akka-http"
  implicit val category: String = "transport"


  implicit def config: ConfigProvider = commonParam.config
  implicit def system: ActorSystem = commonParam.actorSystem
  implicit def materializer: Materializer = commonParam.materializer

  override def start(): Unit = {
    val route: Route = routeParam.routes.sortBy(_.order).map(_.route).reduce(_ ~ _)
    Http().bindAndHandle(corsHandler(route), "0.0.0.0", 6000)
  }

  override def stop(): Unit = {
    //
  }
}