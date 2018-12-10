package com.evernym.agent.core_agent.mediator


import com.evernym.agent.api.{AgentMsgHandler, ConfigProvider, Msg, MsgOrchestrator}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class DefaultMsgOrchestrator(config: ConfigProvider, val agentMsgHandler: AgentMsgHandler) extends MsgOrchestrator {

  def handleMsg(msg: Msg): Future[Any] = {
    Future("ok")
  }
}
