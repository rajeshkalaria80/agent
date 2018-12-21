package com.evernym.agent.common.util

import spray.json._


trait JsonUtilBase extends DefaultJsonProtocol {

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case x: Seq[_] => seqFormat[Any].write(x)
      case m: Map[String, Any] => mapFormat[String, Any].write(m)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
      case other => serializationError("do not understand object of type: " + other.getClass.getName)
    }

    def read(value: JsValue): Any = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case _: JsArray => listFormat[Any].read(value)
      case _: JsObject => mapFormat[String, Any].read(value)
      case JsTrue => true
      case JsFalse => false
      case JsNull => null
      case other => deserializationError("do not understand how to deserialize: " + other)
    }
  }

  def convertJsonToMap(json: String): Map[String, Any] = {
    json.parseJson.convertTo[Map[String, Any]]
  }

  def convertJsonToMapWithStringValue(json: String): Map[String, String] = {
    convertJsonToMap(json).map { kv =>
      val newVal = if (Option(kv._2).isDefined) kv._2.toString else null
      kv._1 ->  newVal
    }
  }

  def convertMapToJson(map: Map[String, Any]): JsValue = {
    map.toJson
  }

  def convertMapToJsonString(map: Map[String, Any]): String = {
    convertMapToJson(map).toString
  }

}


object JsonUtil extends JsonUtilBase