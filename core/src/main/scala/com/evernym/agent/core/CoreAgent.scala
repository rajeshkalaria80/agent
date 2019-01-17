package com.evernym.agent.core

import com.evernym.agent.api._
import com.evernym.agent.core.protocol.agent.CoreAgentOrchestrator

import scala.concurrent.Future


class CoreAgent(val commonParam: CommonParam) extends Agent {
  lazy val agentOrchestrator: AgentOrchestrator = new CoreAgentOrchestrator(commonParam)

  override def handleMsg: PartialFunction[Any, Future[Any]] = {
    case x => agentOrchestrator.handleMsg(x)
  }

  override def start(inputParam: Option[Any]): Unit = {
    agentOrchestrator.start()
  }

  override def stop(): Unit = {
    agentOrchestrator.stop()
  }
}


