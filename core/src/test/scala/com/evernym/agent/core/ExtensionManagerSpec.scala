package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.config.DefaultConfigProvider
import com.evernym.agent.core.extension.{DefaultExtFileFilter, DefaultExtensionManager}
import Constants._
import org.scalatest.{FreeSpec, Matchers}


class ExtensionManagerSpec extends FreeSpec with Matchers {

  val config: ConfigProvider = DefaultConfigProvider

  implicit val actorSystem: ActorSystem = ActorSystem(CORE_AGENT_ACTOR_SYSTEM_NAME)
  implicit val materializer: Materializer = ActorMaterializer()

  val extDirPathsOpt = Option(Set("core/src/main/resources"))
  val extFilterOpt = Option(new DefaultExtFileFilter(config))

  var extManager: ExtensionManager = _
  val agencyInternalApiTransportPlatformExtName = "extension-agency-internal-api-transport-platform"
  val agencyBusinessPlatformExtName = "extension-agency-business-platform"
  val agencyTransportPlatformExtName = "extension-agency-transport-platform"

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
        extManager.getSuccessfullyLoaded.map(_.extension.name) shouldBe
          Set(agencyInternalApiTransportPlatformExtName, agencyBusinessPlatformExtName, agencyTransportPlatformExtName)
      }
    }

    s"when asked to create extension instance for $agencyInternalApiTransportPlatformExtName" - {
      "should be able to create instance of that extension" in {
        val extParam: Option[CommonParam] = Option(CommonParam(config, actorSystem, materializer))
        internalApiExtDetail = extManager.getExtReq(agencyInternalApiTransportPlatformExtName)
        internalApiExtDetail.extension.start(extParam)
        internalApiExtDetail.extension.name shouldBe agencyInternalApiTransportPlatformExtName
        internalApiExtDetail.extension.category shouldBe "transport-platform"
      }
    }

    "when sent msg to internal api extension" - {
      "should get error as it doesn't support any such msg handling" in {
        intercept[RuntimeException] {
          internalApiExtDetail.extension.handleMsg(GenericMsg("test"))
        }
      }
    }

//    s"when asked to create extension instance for $agencyBusinessPlatformExtName" - {
//      "should be able to create instance of that extension" in {
//        val extParam: Option[CommonParam] = Option(CommonParam(config, actorSystem, materializer))
//        agencyApiExtDetail = extManager.getExtReq(agencyBusinessPlatformExtName)
//        agencyApiExtDetail.extension.start(extParam)
//        agencyApiExtDetail.extension.name shouldBe agencyBusinessPlatformExtName
//        agencyApiExtDetail.extension.category shouldBe "business-platform"
//      }
//    }
  }

}
