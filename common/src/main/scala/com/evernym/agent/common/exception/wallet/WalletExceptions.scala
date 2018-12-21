package com.evernym.agent.common.exception.wallet

import com.evernym.agent.common.exception.ExceptionBase

object WalletExceptions extends ExceptionBase {

  class WalletInvalidState(statusCode: String, statusMsg: String,
                        errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletAlreadyExist(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletNotOpened(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletAlreadyOpened(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletNotClosed(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletNotDeleted(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletUnhandledError(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)

  class WalletDoesNotExist(statusCode: String, statusMsg: String,
                           errorDetail: Option[Any] = None)
    extends HandledError(statusCode, statusMsg, errorDetail)
}
