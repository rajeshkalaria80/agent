package com.evernym.agent.core.router


import com.evernym.agent.api.{ConfigProvider, MsgHandler, MsgOrchestrator}
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.common.exception.Exceptions._

import scala.concurrent.Future


class DefaultMsgOrchestrator(config: ConfigProvider, val msgHandlers: Set[MsgHandler]) extends MsgOrchestrator {

  def handleMsg: PartialFunction[Any, Future[Any]] = {
    case m =>
      msgHandlers.find(_.handleMsg.isDefinedAt(m)).map { mh =>
        mh.handleMsg(m)
      }.getOrElse {
        Future.failed(throw new BadRequestError(TBR, s"no handler found for message '$m'"))
      }
  }
}
