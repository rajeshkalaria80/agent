package com.evernym.agent.api

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConverters._


trait ConfigProvider {
  val config: Config = ConfigFactory.load()

  def getStringSet(path: String): Set[String] = {
    config.getStringList(path).asScala.toSet
  }
}
