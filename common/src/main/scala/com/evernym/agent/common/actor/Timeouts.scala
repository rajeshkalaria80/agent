package com.evernym.agent.common.actor

import akka.util.Timeout
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.util.Util.buildTimeout


trait GeneralTimeout {
  def configProvider: ConfigProvider
  implicit lazy val timeout: Timeout = buildTimeout(configProvider,
    "agent.timeouts.akka-actor-msg-reply-timeout", 5)
}
