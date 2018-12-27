package com.evernym.agent.common.exception

trait ExceptionBase {

  abstract class HandledErrorBase(val respCode: String, val respMsg: String, val errorDetail: Option[Any] = None)
    extends RuntimeException(respMsg) {

    override def toString: String = {
      s"respCode: $respCode, respMsg: $respMsg, errorDetail: $errorDetail"
    }

    def getErrorMsg: String = errorDetail.map(_.toString).getOrElse(respMsg)
  }

  class HandledError(respCode: String, respMsg: String, errorDetail: Option[Any] = None)
    extends HandledErrorBase(respCode, respMsg, errorDetail)

  class BadRequestError(code: String, msg: String, errorDetail: Option[Any] = None)
    extends HandledError(code, msg, errorDetail)

  class ConfigLoadingFailed(statusCode: String, statusMsg: String,
                            errorDetail: Option[Any] = None)
    extends BadRequestError(statusCode, statusMsg, errorDetail)

  class InvalidValue(statusCode: String, statusMsg: String,
                     errorDetail: Option[Any] = None)
    extends BadRequestError(statusCode, statusMsg, errorDetail)

  class MissingReqField(statusCode: String, statusMsg: String,
                        errorDetail: Option[Any] = None)
    extends BadRequestError(statusCode, statusMsg, errorDetail)

  class EmptyValueForOptionalField(statusCode: String, statusMsg: String,
                        errorDetail: Option[Any] = None)
    extends BadRequestError(statusCode, statusMsg, errorDetail)

  class InternalServerError(statusCode: String, statusMsg: String,
                        errorDetail: Option[Any] = None)
    extends BadRequestError(statusCode, statusMsg, errorDetail)

  def getErrorMsg(e: Throwable): String = {
    Option(e.getMessage).getOrElse {
      Option(e.getCause).map(_.getMessage).getOrElse(e.toString)
    }
  }
}

object Exceptions extends ExceptionBase