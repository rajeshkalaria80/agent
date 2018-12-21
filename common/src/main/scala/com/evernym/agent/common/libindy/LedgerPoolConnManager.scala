package com.evernym.agent.common.libindy

import com.typesafe.scalalogging.Logger
import com.evernym.agent.api.ConfigProvider
import com.evernym.agent.common.exception.Exceptions
import com.evernym.agent.common.Constants._
import com.evernym.agent.common.util.Util._
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.pool.PoolJSONParameters.CreatePoolLedgerConfigJSONParameter


class LedgerPoolConnManager(val configProvider: ConfigProvider) extends LibIndyCommon {

  private var poolConn: Option[Pool] = None

  val logger: Logger = getLoggerByClass(classOf[LedgerPoolConnManager])

  def getPoolConn: Option[Pool] = poolConn

  def getSafePoolConn: Pool = poolConn.getOrElse(throw new RuntimeException("pool not opened"))

  def isConnected: Boolean = poolConn.isDefined

  private def createPoolLedgerConfig(): Unit = {
    try {
      val createPoolLedgerConfigJSONParameter = new CreatePoolLedgerConfigJSONParameter(genesisTxnFilePath)
      Pool.createPoolLedgerConfig(
        configProvider.getConfigStringReq("TBR"),
        createPoolLedgerConfigJSONParameter.toJson).get
    } catch {
      case e: Throwable =>
        val errorMsg = s"error while creating ledger " +
          s"pool config file (detail => ${Exceptions.getErrorMsg(e)})"
    }
  }

  def open(): Unit = {
    if (poolConn.isEmpty) {
      close()
      deletePoolLedgerConfig()
      createPoolLedgerConfig()
      val ledgerTxnProtocolVer = getLedgerTxnProtocolVersion(configProvider)
      Pool.setProtocolVersion(ledgerTxnProtocolVer).get
      poolConn = Some(Pool.openPoolLedger(configProvider.getConfigStringReq(TBR), "{}").get)
      logger.debug("pool connection established")
    } else {
      logger.debug("pool connection is already established")
    }
  }

  def deletePoolLedgerConfig(): Unit = {
    try {
      Pool.deletePoolLedgerConfig(configProvider.getConfigStringReq(TBR))
    } catch {
      //TODO: Shall we catch some specific exception?
      case _: Throwable =>
        logger.debug("no ledger pool config file to delete")
    }
  }

  def close(): Unit = {
    if (isConnected) {
      poolConn.map(_.closePoolLedger)
      poolConn = None
    }
  }

}

class BasePoolConnectionException extends Throwable {
  val message: String = ""

  override def getMessage: String = message
}

case class PoolConnectionNotOpened(override val message: String = "") extends BasePoolConnectionException