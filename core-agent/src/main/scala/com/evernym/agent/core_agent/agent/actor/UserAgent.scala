package com.evernym.agent.core_agent.agent.actor


case class SetName (name: String)

case class NameSet(name: String) extends Event

class UserAgent extends PersistentActorBase {

  var name: Option[String] = None

  override val receiveRecover: Receive = {
    case ns: NameSet => name = Option(ns.name)
  }

  override val receiveCommand: Receive = {
    case sn: SetName =>
      writeAndApply(NameSet(sn.name))
  }
}
