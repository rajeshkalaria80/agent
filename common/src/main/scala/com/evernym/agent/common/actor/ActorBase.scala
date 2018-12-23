package com.evernym.agent.common.actor

import akka.persistence.PersistentActor


trait Event


trait ActorBase


trait PersistentActorBase extends ActorBase with PersistentActor { this: PersistentActor =>

  val entityId: String = self.path.name
  val entryName: String = self.path.parent.parent.name
  override val persistenceId: String = entryName + "-" + entityId

  def emptyEventHandler(event: Any): Unit = {}

  private def writeWithoutApply(evt: Event): Unit = persist(evt)(emptyEventHandler)

  def apply(evt: Event): Unit = receiveRecover(evt)

  def writeAndApply(evt: Event): Unit= {
    writeWithoutApply(evt)
    apply(evt)
  }

  def writeApplyAndSendItBack(evt: Event): Unit = {
    writeAndApply(evt)
    sender ! evt
  }
}
