package com.evernym.agent.common.actor

import akka.actor.Props
import com.evernym.agent.api.ConfigProvider


//cmd
case class StoreValue(key: String, value: String)
case class GetValue(key: String)


object KeyValueStore {
  def props(configProvider: ConfigProvider) = Props(new KeyValueStore(configProvider))
}


class KeyValueStore(configProvider: ConfigProvider) extends PersistentActorBase {

  var values: Map[String, String] = Map.empty

  override def receiveRecover: Receive = {
    case vs: ValueStored => values += vs.key -> vs.value
  }

  override def receiveCommand: Receive = {
    case sv: StoreValue => writeApplyAndSendItBack(ValueStored(sv.key, sv.value))
    case gv: GetValue => sender ! values.get(gv.key)
  }
}
