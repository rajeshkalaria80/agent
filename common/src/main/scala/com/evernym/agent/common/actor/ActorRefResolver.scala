package com.evernym.agent.common.actor

import akka.actor.{ActorNotFound, ActorRef, ActorSystem}
import com.evernym.agent.api.{CommonParam, ConfigProvider}
import com.evernym.agent.common.util.Util.getActorRefFromSelection

trait ActorRefResolver extends GeneralTimeout {

  implicit def param: CommonParam
  implicit def config: ConfigProvider = param.config
  implicit def system: ActorSystem = param.actorSystem

  val ACTOR_PATH_PREFIX = "/user"
  val USER_AGENT_ACTOR_NAME = "18566eae-470d-4574-837f-401e1e6dda16"

  private var resolvedActorRefs: Map[String, ActorRef] = Map.empty

  private def getFromCache(id: String): Option[ActorRef] = resolvedActorRefs.get(id)

  private def resolveAndCache(id: String): Option[ActorRef] = {
    try {
      val ar = getActorRefFromSelection(s"$ACTOR_PATH_PREFIX/$id", system)
      resolvedActorRefs += id -> ar
      Option(ar)
    } catch {
      case _: ActorNotFound =>
        None
    }
  }

  private def getActorRefOpt(id: String): Option[ActorRef] = getFromCache(id) orElse resolveAndCache(id)

  private def getActorRefReq(id: String): ActorRef = {
    getActorRefOpt(id).getOrElse(throw new RuntimeException("required actor is not created"))
  }


  def userAgentActorRefOpt: Option[ActorRef] = getActorRefOpt(USER_AGENT_ACTOR_NAME)

  def userAgentPairwiseActorRefOpt(forAgentPairwiseId: String): Option[ActorRef] = getActorRefOpt(forAgentPairwiseId)

  def getUserAgentActorRefReq: ActorRef = getActorRefReq(USER_AGENT_ACTOR_NAME)

  def getUserAgentPairwiseActorRefReq(forAgentPairwiseId: String): ActorRef = getActorRefReq(forAgentPairwiseId)

}