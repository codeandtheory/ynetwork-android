package com.yml.network.core.response

/**
 * Exception class for the network request failure with error status code.
 */
data class StatusCodeException(val statusCode: HttpStatusCode, val errorMessage: String?) : Exception()

/**
 * Exception class when there is no response for the network request.
 */
class NoDataException() : Exception()
