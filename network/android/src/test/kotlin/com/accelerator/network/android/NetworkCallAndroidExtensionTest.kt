package com.accelerator.network.android

import androidx.lifecycle.Observer
import com.yml.network.core.NetworkCall
import com.yml.network.core.Resource
import com.yml.network.core.response.DataResponse
import com.yml.network.core.response.DataSource
import com.yml.network.core.response.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi
@ExtendWith(InstantExecutorExtension::class)
class NetworkCallAndroidExtensionTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLiveData() = runBlocking {
        val data1 = Resource.Loading(data = createDataResponse("a"))
        val flow = MutableStateFlow<Resource<DataResponse<String>>>(data1)

        val call = NetworkCall(flow)
        val liveData = call.asLiveData(dispatcher)

        val observer = mockk<Observer<Resource<DataResponse<String>>>>()
        every { observer.onChanged(any()) } returns Unit
        liveData.observeForever(observer)

        val data2 = Resource.Success(createDataResponse("abc"))
        flow.emit(data2)

        // Verify LiveData's observer is called.
        verify { observer.onChanged(data2) }

        // Cleanup
        liveData.removeObserver(observer)
    }

    private fun createDataResponse(data: String) =
        DataResponse(data, null, DataSource.Network, HttpStatusCode.OK)
}