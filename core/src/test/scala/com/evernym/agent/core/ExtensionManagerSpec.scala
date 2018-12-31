package com.evernym.agent.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import com.evernym.agent.common.config.DefaultConfigProvider
import com.evernym.agent.core.extension.{DefaultExtFileFilter, DefaultExtensionManager}
import com.evernym.agent.core.common.Constants._
import org.scalatest.{FreeSpec, Matchers}


class ExtensionManagerSpec extends FreeSpec with Matchers {

  val config: ConfigProvider = DefaultConfigProvider

  implicit val actorSystem: ActorSystem = ActorSystem(CORE_AGENT_ACTOR_SYSTEM_NAME)
  implicit val materializer: Materializer = ActorMaterializer()

  val extDirPathsOpt = Option(Set("core/src/main/resources"))
  val extFilterOpt = Option(new DefaultExtFileFilter(config))

  var extManager: ExtensionManager = _
  val extInternalApiName = "agent-ext-akka-http-internal-api"
  val extAgencyApiName = "agent-ext-agency-api"
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
        extManager.getSuccessfullyLoaded.map(_.extension.name) shouldBe Set(extInternalApiName, extAgencyApiName)
        extManager.getErrors shouldBe Set.empty
      }
    }

    s"when asked to create extension instance for $extInternalApiName" - {
      "should be able to create instance of that extension" in {
        val extParam: Option[CommonParam] = Option(CommonParam(config, actorSystem, materializer))
        internalApiExtDetail = extManager.getExtReq(extInternalApiName)
        internalApiExtDetail.extension.init(extParam)
        internalApiExtDetail.extension.name shouldBe extInternalApiName
        internalApiExtDetail.extension.category shouldBe "transport.http.akka"
      }
    }

    "when sent msg to internal api extension" - {
      "should get error as it doesn't support any such msg handling" in {
        intercept[RuntimeException] {
          internalApiExtDetail.extension.handleMsg(TransportAgnosticMsg("test", Option(MsgInfoReq("1.2.3.4"))))
        }
      }
    }

//    s"when asked to create extension instance for $extAgencyApiName" - {
//      "should be able to create instance of that extension" in {
//        val extParam: Option[CommonParam] = Option(CommonParam(config, actorSystem, materializer))
//        agencyApiExtDetail = extManager.getExtReq(extAgencyApiName)
//        agencyApiExtDetail.extension.init(extParam)
//        agencyApiExtDetail.extension.name shouldBe extAgencyApiName
//        agencyApiExtDetail.extension.category shouldBe "transport.http.akka"
//      }
//    }
  }

}
