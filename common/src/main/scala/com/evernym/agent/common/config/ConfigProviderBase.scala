package com.evernym.agent.common.config

import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.CommonConstants.TBR
import com.evernym.agent.common.exception.Exceptions._
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import scala.collection.JavaConverters._


trait ConfigProviderBase extends ConfigProvider {

  val config: Config = ConfigFactory.load()

  private def readReqConfig[T](f: String => T, key: String): T = {
    try {
      f(key)
    } catch {
      case _: Missing ⇒
        throw new ConfigLoadingFailed(TBR, s"required config not found: $key")
      case _: ConfigException ⇒
        throw new ConfigLoadingFailed(TBR, s"required config not found or has invalid value: $key")
    }
  }

  private def readOptionalConfig[T](f: String => T, key: String): Option[T] = {
    try {
      Option(f(key))
    } catch {
      case _: Missing ⇒ None
    }
  }

  def getStringSet(path: String): Set[String] = {
    config.getStringList(path).asScala.toSet
  }

  def getConfigIntReq(key: String): Int = {
    readReqConfig(config.getInt, key)
  }

  def getConfigIntOption(key: String): Option[Int] = {
    readOptionalConfig(config.getInt, key)
  }

  def getConfigStringOption(key: String): Option[String] = {
    readOptionalConfig(config.getString, key)
  }

  def getConfigStringReq(key: String): String = {
    readReqConfig(config.getString, key)
  }

}
