package com.evernym.agent.common.actor

import akka.serialization.SerializerWithStringManifest


class ProtoBufSerializer
  extends SerializerWithStringManifest {

  override def identifier: Int = 99

  //mostly we we'll have only one event (TransformedEvent) which will come at this level for serialization,
  // so if we want, we can just use empty string as manifest and it will work
  // but just in case if we have to support more events in future,
  // we can give numbers to different types of events.

  val TRANSFORMED_EVENT_MANIFEST = "1"

  override def manifest(o: AnyRef): String = {
    o match {
      case _: TransformedEvent ⇒ TRANSFORMED_EVENT_MANIFEST
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case etd: TransformedEvent => etd.toByteArray
    }
  }

  def fromBinary(
                  bytes: Array[Byte],
                  manifest: String
                ): AnyRef = {
    manifest match {
      case TRANSFORMED_EVENT_MANIFEST => TransformedEvent.parseFrom(bytes)
    }
  }
}