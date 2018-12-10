package com.evernym.agent.core_agent.agent.actor

import com.evernym.agent.api.{Extension, ExtensionManager, ExtensionParam, ExtensionWrapper}


class DefaultExtensionManager extends ExtensionManager {

  var loaded: Map[String, ExtensionWrapper] = Map.empty
  var activated: Map[String, Extension] = Map.empty

  def load(dirPaths: Set[String]): Map[String, ExtensionWrapper] = {
    Map.empty
  }

  override def getLoaded: Map[String, ExtensionWrapper] = loaded

  def activate(name: String, param: ExtensionParam): Unit = {
    loaded.get(name).foreach { ew =>
      activated = activated ++ Map(name -> ew.create(param))
    }
  }

  def deactivate(name: String): Unit = {
    activated = activated.filterNot(_._1 == name)
  }

  def getActivatedExtension: Map[String, Extension] = activated
}
