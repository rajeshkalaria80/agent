package com.evernym.agent.core.common

import com.evernym.agent.common.util.TransformationUtilBase
import com.evernym.agent.common.exception.Exceptions._
import com.evernym.agent.common.CommonConstants._
import com.evernym.agent.core.Constants._
import com.evernym.agent.core.actor.AgentDetailSet
import spray.json.RootJsonFormat

case class InitAgent(DID: String, verKey: String)

case class RouteDetail(persistenceId: String, actorTypeId: Int)

trait MsgBase {

  def throwMissingReqFieldException(fieldName: String): Unit = {
    throw new MissingReqField(TBR, s"required attribute not found (missing/empty/null): '$fieldName'")
  }

  def throwOptionalFieldValueAsEmptyException(fieldName: String): Unit = {
    throw new EmptyValueForOptionalField(TBR, s"empty value given for optional field: '$fieldName'")
  }

  def checkRequired(fieldName: String, fieldValue: Any): Unit = {
    if (Option(fieldValue).isEmpty) throwMissingReqFieldException(fieldName)
  }

  def checkRequired(fieldName: String, fieldValue: List[Any]): Unit = {
    if (Option(fieldValue).isEmpty || fieldValue.isEmpty)
      throwMissingReqFieldException(fieldName)
  }

  def checkRequired(fieldName: String, fieldValue: String): Unit = {
    val fv = Option(fieldValue)
    if (fv.isEmpty || fv.exists(_.trim.length == 0)) throwMissingReqFieldException(fieldName)
  }

  def checkOptionalNotEmpty(fieldName: String, fieldValue: Option[Any]): Unit = {
    fieldValue match {
      case Some(s: String) => if (s.trim.length ==0) throwOptionalFieldValueAsEmptyException(fieldName)
      case Some(_: Any) => //
      case _ => //
    }
  }
}

trait RespMsgBase extends MsgBase

case class TypeDetail(name: String, ver: String, fmt: Option[String]=None) extends MsgBase {
  checkOptionalNotEmpty("fmt", fmt)
}

case class AgentCreatedRespMsg(`@type`: TypeDetail, agentPairwiseDID: String, agentPairwiseDIDVerKey: String) extends RespMsgBase

trait JsonTransformationUtil extends TransformationUtilBase {

  implicit val version: String = "1.0"

  private def buildAgentCreatedTypeDetail(ver: String): TypeDetail = TypeDetail(MSG_TYPE_AGENT_CREATED, ver)

  def buildAgentCreatedRespMsg(toDID: String, toDIDVerKey: String)(implicit ver: String): AgentCreatedRespMsg = {
    AgentCreatedRespMsg(buildAgentCreatedTypeDetail(ver), toDID, toDIDVerKey)
  }

  implicit val rd: RootJsonFormat[RouteDetail] = jsonFormat2(RouteDetail.apply)
  implicit val initAgent: RootJsonFormat[InitAgent] = jsonFormat2(InitAgent.apply)
  implicit val agentDetailSet: RootJsonFormat[AgentDetailSet] = jsonFormat2(AgentDetailSet.apply)

  implicit val typeDetailMsg: RootJsonFormat[TypeDetail] = jsonFormat3(TypeDetail.apply)
  implicit val agentCreatedRespMsg: RootJsonFormat[AgentCreatedRespMsg] = jsonFormat3(AgentCreatedRespMsg.apply)
}
