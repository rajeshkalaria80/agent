package com.evernym.agent.core.transport.http.akka

import com.evernym.agent.common.util.TransformationUtilBase
import com.evernym.agent.core.msg_handler.RouteDetail
import com.evernym.agent.core.msg_handler.actor.AgentDetailSet
import spray.json.RootJsonFormat

case class InitAgent(DID: String, verKey: String)

trait JsonTransformationUtil extends TransformationUtilBase {

  implicit val rd: RootJsonFormat[RouteDetail] = jsonFormat2(RouteDetail.apply)
  implicit val initAgent: RootJsonFormat[InitAgent] = jsonFormat2(InitAgent.apply)
  implicit val agentDetailSet: RootJsonFormat[AgentDetailSet] = jsonFormat2(AgentDetailSet.apply)
}
