package com.evernym.agent.common.actor

import akka.actor.{ActorNotFound, ActorRef, ActorSystem}
import com.evernym.agent.api.{CommonParam, ConfigProvider}
import com.evernym.agent.common.util.Util.getActorRefFromSelection


trait ActorRefResolver extends GeneralTimeout {

  implicit def param: CommonParam
  implicit def config: ConfigProvider = param.configProvider
  implicit def system: ActorSystem = param.actorSystem

  private var resolvedActorRefs: Map[String, ActorRef] = Map.empty

  private def getFromCache(id: String): Option[ActorRef] = resolvedActorRefs.get(id)

  private def resolveAndCache(id: String, path: String): Option[ActorRef] = {
    try {
      val ar = getActorRefFromSelection(path, system)
      resolvedActorRefs += id -> ar
      Option(ar)
    } catch {
      case _: ActorNotFound =>
        None
    }
  }

  private def getActorRefOpt(id: String, path: String): Option[ActorRef] =
    getFromCache(id) orElse resolveAndCache(id, path)

  private def getActorRefReq(id: String, path: String): ActorRef = {
    getActorRefOpt(id, path).getOrElse(throw new RuntimeException("required actor is not created"))
  }

  def agentActorRefOpt(id: String, path: String): Option[ActorRef] = getActorRefOpt(id, path)

  def agentActorRefReq(id: String, path: String): ActorRef = getActorRefReq(id, path)

}
