package com.evernym.agent.core.common

import akka.util.Timeout
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.Constants.TBR
import com.evernym.agent.common.util.Util.buildTimeout


trait GeneralTimeout {
  def config: ConfigProvider
  implicit lazy val timeout: Timeout = buildTimeout(config, TBR, 10)
}
