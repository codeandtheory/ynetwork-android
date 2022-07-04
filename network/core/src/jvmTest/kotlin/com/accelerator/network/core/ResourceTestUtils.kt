package com.accelerator.network.core

import com.accelerator.network.core.response.DataResponse
import com.accelerator.network.core.response.DataSource
import com.accelerator.network.core.response.HttpStatusCode
import com.accelerator.network.core.response.StatusCodeException
import kotlin.reflect.KClass
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.core.Is.`is`

fun <DATA> verifyLoadingResource(
    result: Resource<DataResponse<DATA>>,
    data: DataResponse<DATA>? = null,
    error: Exception? = null
) {
    assertThat(result, `is`(instanceOf(Resource.Loading::class.java)))
    val loadingResource = result as Resource.Loading<DataResponse<DATA>>
    assertThat(loadingResource.data, `is`(data))
    assertThat(loadingResource.error, `is`(error))
}

fun <DATA> verifySuccessResource(
    result: Resource<DataResponse<DATA>>,
    body: DATA,
    source: DataSource = DataSource.Network,
    statusCode: HttpStatusCode = HttpStatusCode.OK
) {
    assertThat(result, `is`(instanceOf(Resource.Success::class.java)))
    val dataResponse = (result as Resource.Success<DataResponse<DATA>>).data
    assertThat(dataResponse.source, `is`(source))
    assertThat(dataResponse.statusCode, `is`(statusCode))
    assertThat(dataResponse.body, `is`(body))
}

fun <DATA> verifyStatusCodeErrorResource(
    result: Resource<DataResponse<DATA>>,
    httpStatusCode: HttpStatusCode,
    errorMessage: String
) {
    assertThat(result, `is`(instanceOf(Resource.Error::class.java)))
    val exception = (result as Resource.Error<DataResponse<DATA>>).error
    assertThat(exception, `is`(instanceOf(StatusCodeException::class.java)))

    val statusCodeException = exception as StatusCodeException
    assertThat(statusCodeException.statusCode, `is`(httpStatusCode))
    assertThat(statusCodeException.errorMessage, `is`(errorMessage))
}

fun <DATA, EXCEPTION : Any> verifyErrorResource(
    result: Resource<DataResponse<DATA>>,
    exceptionClass: KClass<EXCEPTION>,
    message: String
) {
    assertThat(result, `is`(instanceOf(Resource.Error::class.java)))
    val exception = (result as Resource.Error<DataResponse<DATA>>).error
    assertThat(exception, `is`(instanceOf(exceptionClass.java)))
    assertThat(exception?.message, `is`(message))
}
