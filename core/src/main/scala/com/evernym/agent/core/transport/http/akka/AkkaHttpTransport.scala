package com.evernym.agent.core.transport.http.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives.{complete, logRequestResult, options, path, pathPrefix, post}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import akka.stream.Materializer
import com.evernym.agent.api._
import com.evernym.agent.common.a2a.AuthCryptedMsg
import com.evernym.agent.core.common.{InitAgent, JsonTransformationUtil}

import scala.concurrent.ExecutionContextExecutor


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

class DefaultTransportParamHttpAkka(val commonParam: CommonParam, val transportMsgRouter: TransportMsgRouter)
  extends TransportHttpAkkaRouteParam with JsonTransformationUtil {

  implicit val executor: ExecutionContextExecutor = commonParam.actorSystem.dispatcher

  def msgResponseHandler: PartialFunction[Any, ToResponseMarshallable] = {
    case a2aMsg: AuthCryptedMsg =>
      HttpEntity(MediaTypes.`application/octet-stream`, a2aMsg.payload)
  }

  lazy val coreAgentRoute: Route = logRequestResult("core-agent-service") {
    pathPrefix("agent") {
      path("init") {
        (post & entity(as[InitAgent])) { ai =>
          complete {
            transportMsgRouter.handleMsg(TransportAgnosticMsg(ai)).map[ToResponseMarshallable] {
              msgResponseHandler
            }
          }
        }
      } ~
        path("msg") {
          extractRequest { implicit req: HttpRequest =>
            post {
              import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers.byteArrayUnmarshaller
              req.entity.contentType.mediaType match {
                case MediaTypes.`application/octet-stream` =>
                  entity(as[Array[Byte]]) { data =>
                    complete {
                      transportMsgRouter.handleMsg(TransportAgnosticMsg(AuthCryptedMsg(data))).map[ToResponseMarshallable] {
                        msgResponseHandler
                      }
                    }
                  }
                case _ => reject
              }
            }
          }
        }
    }
  }

  override lazy val route: Route = coreAgentRoute
}


class CoreAgentTransportAkkaHttp(val commonParam: CommonParam, val routeParam: TransportHttpAkkaRouteParam)
  extends Transport with CorsSupport {

  implicit val name: String = "akka-http"
  implicit val category: String = "transport"


  implicit def config: ConfigProvider = commonParam.config
  implicit def system: ActorSystem = commonParam.actorSystem
  implicit def materializer: Materializer = commonParam.materializer

  override def start(): Unit = {
    val route: Route = routeParam.route
    Http().bindAndHandle(corsHandler(route), "0.0.0.0", 6000)
  }

  override def stop(): Unit = {
    //
  }
}