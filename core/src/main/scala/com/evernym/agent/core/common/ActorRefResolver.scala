package com.evernym.agent.core.common

import akka.actor.{ActorRef, ActorSystem}
import com.evernym.agent.api.{CommonParam, ConfigProvider}
import com.evernym.agent.common.util.Util.getActorRefFromSelection


trait ActorRefResolver extends GeneralTimeout {

  implicit def param: CommonParam
  implicit def config: ConfigProvider = param.config
  implicit def system: ActorSystem = param.actorSystem

  val ACTOR_PATH_PREFIX = "/user"
  val USER_AGENT_ACTOR_NAME = "UserAgent"

  private var resolvedActorRefs: Map[String, ActorRef] = Map.empty

  private def getFromResolved(id: String): Option[ActorRef] = resolvedActorRefs.get(id)

  private def resolveAndCache(id: String): ActorRef = {
    val ar = getActorRefFromSelection(s"$ACTOR_PATH_PREFIX/$id", system)
    resolvedActorRefs += id -> ar
    ar
  }

  private def getActorRef(id: String): ActorRef = getFromResolved(id).getOrElse(resolveAndCache(id))

  def userAgent: ActorRef = getActorRef(USER_AGENT_ACTOR_NAME)

  def userAgentPairwise(forAgentPairwiseId: String): ActorRef = getActorRef(forAgentPairwiseId)

}
