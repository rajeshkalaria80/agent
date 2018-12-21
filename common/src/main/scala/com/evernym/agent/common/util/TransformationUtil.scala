package com.evernym.agent.common.util

import java.time.ZonedDateTime

import com.evernym.agent.common.CommonConstants.TBR
import com.evernym.agent.common.exception.Exceptions._
import com.evernym.agent.common.util.Util._
import spray.json._


trait TransformationUtilBase extends JsonUtilBase {

  //NOTE NOTE NOTE:
  //seems order also does matter, so child classes should be declared before the parent case classes (for converting to json)
  implicit val ZonedDateTimeFormat:RootJsonFormat[ZonedDateTime] = new RootJsonFormat[ZonedDateTime] {
    //TODO: read is not compatible with write, but for now, we only need to use write
    def read(json: JsValue): ZonedDateTime = getZonedDateTimeFromMillis(json.toString.toLong)(getUTCZoneId)
    def write(zonedDateTime: ZonedDateTime) = JsString(zonedDateTime.toString)
  }

  def convertJsonToNativeMsg[T](jsonString: String)(implicit rjf: RootJsonFormat[T]): T = {
    try {
      val reqJsValue = jsonString.parseJson
      reqJsValue.convertTo[T]
    } catch {
      case e: DeserializationException if e.getMessage.startsWith("Object is missing required member") =>
        throw new MissingReqField(TBR, s"required attribute not found (missing/empty/null): '$jsonString' (${e.getMessage})")
    }
  }
}

object TransformationUtil extends TransformationUtilBase