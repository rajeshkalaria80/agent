package com.evernym.agent.common.transport

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, GatewayTimeout, OK}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json.RootJsonFormat
import com.evernym.agent.common.util.Util.buildDurationInSeconds
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol

import scala.concurrent.{Await, Future}
import scala.util.Left

case class Endpoint(host: String, port: Int, path: String) {
  override def toString: String = host + ":" + port + "/" + path
}

trait RemoteMsgSendingSvc {
  def sendAsyncMsgExpectingBinaryResponse(payload: Any)(implicit ep: Endpoint): Future[Either[String, Array[Byte]]]
  def sendAsyncMsgExpectingStringResponse(payload: Any)(implicit ep: Endpoint): Future[Either[String, String]]
  def sendSyncMsgExpectingStringResponse[T](msg: T, path: String=null)(implicit ep: Endpoint, rjf: RootJsonFormat[T]): String
  def sendSyncMsgExpectingBinaryResponse(msg: Array[Byte])(implicit ep: Endpoint): Array[Byte]
}

trait HttpRemoteMsgSendingSvc extends RemoteMsgSendingSvc with DefaultJsonProtocol {
  import scala.concurrent.ExecutionContext.Implicits.global
  import spray.json._

  val logger = Logger("test")

  def config: Config
  implicit def actorSystem: ActorSystem
  implicit def materializer: Materializer = ActorMaterializer()

  private def getConnection(ep: Endpoint): Flow[HttpRequest, HttpResponse, Any] = {
    Http().outgoingConnection(ep.host, ep.port)
  }

  private def sendHttpRequest(request: HttpRequest)(implicit ep: Endpoint): Future[HttpResponse] =
    Source.single(request).via(getConnection(ep)).runWith(Sink.head).recover {
      case _ =>
        val errMsg = s"connection not established with remote server: ${ep.toString}"
        logger.error(errMsg)
        HttpResponse(StatusCodes.custom(GatewayTimeout.intValue, errMsg, errMsg))
    }

  private def parseResponse[T](response: HttpResponse)(implicit ep: Endpoint,
                                               um: Unmarshaller[ResponseEntity, T]): Future[Either[String, T]] = {
    response.status match {
      case OK =>
        logger.debug(s"ok response received from '${ep.toString}'")
        Unmarshal(response.entity).to[T].map(Right(_))
      case BadRequest =>
        logger.error(s"bad-request response received from '${ep.toString}'")
        Unmarshal(response.entity).to[String].map(Left(_))
      case e =>
        val error = s"error while sending message to '${ep.toString}': $e"
        logger.error(error)
        Unmarshal(response.entity).to[String].map(Left(_))
    }
  }

  private def sendMsgToRemoteEndpoint[T](entity: HttpEntity.Strict)
                                (implicit ep: Endpoint,
                                 um: Unmarshaller[ResponseEntity, T]): Future[Either[String, T]] = {
    val req = HttpRequest(
      method = HttpMethods.POST,
      uri = s"/${ep.path}",
      entity = entity
    )
    sendHttpRequest(req).flatMap { response =>
      parseResponse(response)
    }
  }

  def buildEntity(payload: Any): HttpEntity.Strict = {
    payload match {
      case s: String => HttpEntity(MediaTypes.`application/json`, payload.asInstanceOf[String])
      case bd: Array[Byte] => HttpEntity(MediaTypes.`application/octet-stream`, payload.asInstanceOf[Array[Byte]])
    }
  }

  override def sendAsyncMsgExpectingBinaryResponse(payload: Any)
                                                  (implicit ep: Endpoint): Future[Either[String, Array[Byte]]] = {
    sendMsgToRemoteEndpoint[Array[Byte]](buildEntity(payload))
  }

  override def sendAsyncMsgExpectingStringResponse(payload: Any)
                                                  (implicit ep: Endpoint): Future[Either[String, String]] = {
    sendMsgToRemoteEndpoint[String](buildEntity(payload))
  }

  override def sendSyncMsgExpectingStringResponse[T](msg: T, path: String=null)
                                                    (implicit ep: Endpoint, rjf: RootJsonFormat[T]): String = {

    val finalEndpoint = if (Option(path).isEmpty) ep else ep.copy(path=path)
    val futResp = sendAsyncMsgExpectingStringResponse(msg.toJson.toString)(finalEndpoint)
    val resp = Await.result(futResp, buildDurationInSeconds(5))
    resp match {
      case Right(bd: String) => bd
      case Left(e) => throw new RuntimeException(e)
    }
  }

  override def sendSyncMsgExpectingBinaryResponse(msg: Array[Byte])(implicit ep: Endpoint): Array[Byte] = {

    val futResp = sendAsyncMsgExpectingBinaryResponse(msg)(ep)
    val resp = Await.result(futResp, buildDurationInSeconds(5))
    resp match {
      case Right(bd: Array[Byte]) => bd
      case Left(e) => throw new RuntimeException(e)
    }
  }

}
