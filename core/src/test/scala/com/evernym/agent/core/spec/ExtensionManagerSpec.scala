package com.evernym.agent.core.spec

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.config.DefaultConfigProvider
import com.evernym.agent.common.extension.{DefaultExtFileFilter, DefaultExtensionManager}
import com.evernym.agent.core.Constants._
import org.scalatest.{FreeSpec, Matchers}


class ExtensionManagerSpec extends FreeSpec with Matchers {

  val config: ConfigProvider = DefaultConfigProvider

  implicit val actorSystem: ActorSystem = ActorSystem(CORE_AGENT_ACTOR_SYSTEM_NAME)
  implicit val materializer: Materializer = ActorMaterializer()

  val extDirPathsOpt = Option(Set("core/src/main/resources"))
  val extFilterOpt = Option(new DefaultExtFileFilter(config))

  var extManager: ExtensionManager = _
  val agencyInternalApiTransportProtocolExtName = "extension-agency-internal-api-transport-protocol"
  val agencyBusinessProtocolExtName = "extension-agency-business-protocol"
  val agencyTransportProtocolExtName = "extension-agency-transport-protocol"

  var internalApiExtDetail: ExtensionDetail = _
  var agencyApiExtDetail: ExtensionDetail = _

  "An extension manager" - {

    "when created first time" - {
      "should be created successfully" in {
        extManager = new DefaultExtensionManager(config)
      }
    }

    "when asked to load extensions" - {
      "should be able to load it successfully" in {
        extManager.load(extDirPathsOpt, extFilterOpt)
        extManager.getErrors shouldBe Set.empty
        extManager.getSuccessfullyLoaded.map(_.extension.name) shouldBe Set.empty
      }
    }
  }
}
