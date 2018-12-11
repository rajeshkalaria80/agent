package com.evernym.agent.core_agent

import com.evernym.agent.api.{Extension, ExtensionManager, ExtensionParam}
import org.scalatest.{FreeSpec, Matchers}


class ExtensionManagerSpec extends FreeSpec with Matchers {

  val config = new DefaultConfigProvider()
  val extFilter = new DefaultExtensionFilter(config)
  var extManager: ExtensionManager = _
  val extDirPath = "/home/rkalaria/dev/evernym/agent/core-agent/src/test/test-extensions"

  "An extension manager" - {

    "when created first time" - {
      "should be created successfully" in {
        extManager = new DefaultExtensionManager(config)
      }
    }

    "when asked to load extensions" - {
      "should be able to load it successfully" in {
        extManager.load(Set(extDirPath), extFilter)
        extManager.getLoadedNames shouldBe Set("agent-ext-akka-http-internal-api")
      }
    }

    "when asked to create extension instance" - {
      "should be able to create instance of that extension" in {
        val ext = extManager.createExtension("agent-ext-akka-http-internal-api", None)
        ext.isInstanceOf[Extension]
        ext.getDisplayName shouldBe "agent-ext-internal-api"
      }
    }

  }

}
