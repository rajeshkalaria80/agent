package com.evernym.agent.common.libindy

import com.evernym.agent.api.ConfigProvider
import com.typesafe.scalalogging.Logger
import com.evernym.agent.common.util.Util._
import org.hyperledger.indy.sdk.LibIndy


trait LibIndyCommon {

  def configProvider: ConfigProvider

  val liLogger: Logger = getLoggerByClass(classOf[LibIndyCommon])

  val libIndyDirPath: String = {
    val lifp = configProvider.getConfigStringReq("agent.libindy.library-dir-location")
    liLogger.debug("lib indy dir path: " + lifp)
    lifp
  }
  val genesisTxnFilePath: String = {
    val gptf = configProvider.getConfigStringReq("agent.libindy.ledger.genesis-txn-file-location")
    liLogger.debug("pool txn file path: " + gptf)
    gptf
  }

  LibIndy.init(libIndyDirPath)
}
