package com.evernym.agent.core_agent

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.evernym.agent.api._
import org.scalatest.{FreeSpec, Matchers}


class ExtensionManagerSpec extends FreeSpec with Matchers {

  val config = new DefaultConfigProvider()
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  val extFilter = new DefaultExtensionFilter(config)
  var extManager: ExtensionManager = _
  val extDirPath = "/home/rkalaria/dev/evernym/agent/core-agent/src/test/test-extensions"
  val extInternalApiName = "agent-ext-akka-http-internal-api"
  var internalApiExtWrapper: ExtensionWrapper = _

  "An extension manager" - {

    "when created first time" - {
      "should be created successfully" in {
        extManager = new DefaultExtensionManager(config)
      }
    }

    "when asked to load extensions" - {
      "should be able to load it successfully" in {
        extManager.load(Set(extDirPath), extFilter)
        extManager.getExtWrappers.keySet shouldBe Set(extInternalApiName)
      }
    }

    "when asked to create extension instance" - {
      "should be able to create instance of that extension" in {
        val extParam: Option[CommonParam] = Option(CommonParam(config, actorSystem, materializer))
        internalApiExtWrapper = extManager.getExtWrapperReq(extInternalApiName)
        internalApiExtWrapper.createExtension(extParam)
        internalApiExtWrapper.name shouldBe "internal-api"
        internalApiExtWrapper.category shouldBe "transport.http.akka"

      }
    }

    "when sent msg to internal api extension" - {
      "should get error as it doesn't support any such msg handling" in {
        intercept[RuntimeException] {
          internalApiExtWrapper.handleMsg(Msg("test", MsgInfoReq()))
        }
      }
    }

  }

}
