package com.evernym.agent.core.common

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.evernym.agent.api.{CommonParam, ConfigProvider}
import com.evernym.agent.common.Constants.TBR
import com.evernym.agent.common.util.Util.{buildTimeout, getActorRefFromSelection}


trait ActorRefResolver extends GeneralTimeout {

  implicit def param: CommonParam
  implicit def config: ConfigProvider = param.config
  implicit def system: ActorSystem = param.actorSystem

  val ACTOR_PATH_PREFIX = "/user"
  val USER_AGENT_ACTOR_NAME = "UserAgent"
  val USER_AGENT_PAIRWISE_ACTOR_NAME = "UserAgentPairwise"

  lazy val userAgent: ActorRef =
    getActorRefFromSelection(s"$ACTOR_PATH_PREFIX/$USER_AGENT_ACTOR_NAME", system)

  lazy val userAgentPairwise: ActorRef =
    getActorRefFromSelection(s"$ACTOR_PATH_PREFIX/$USER_AGENT_PAIRWISE_ACTOR_NAME", system)

}
