package com.accelerator.network.core.response

import com.accelerator.network.core.Headers

/**
 * data class holding response for network request
 *
 * @property body response body for network request
 * @property headers HTTP header that can be used in an HTTP response to pass additional information
 * @property source defines the source of data
 * @property statusCode codes indicate whether a specific HTTP request has been successfully completed
 *
 */
data class DataResponse<RESPONSE>(
    val body: RESPONSE?,
    val headers: Headers?,
    val source: DataSource,
    val statusCode: HttpStatusCode?
)
