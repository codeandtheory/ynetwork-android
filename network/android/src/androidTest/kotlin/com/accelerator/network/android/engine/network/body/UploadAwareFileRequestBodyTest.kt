package com.accelerator.network.android.engine.network.body

import com.yml.network.core.MimeType
import com.yml.network.core.Resource
import com.yml.network.core.request.FileTransferInfo
import com.yml.network.core.request.FileTransferProgressCallback
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import okio.BufferedSink
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.nio.charset.StandardCharsets

private val TEST_DATA = "A".repeat(SEGMENT_SIZE.toInt())

private const val FILE_NAME = "testData.txt"

class UploadAwareFileRequestBodyTest {
    @get:Rule
    var tempDir = TemporaryFolder()

    private lateinit var testDataFolder: File

    @Before
    fun setup() {
        testDataFolder = tempDir.newFolder("uploadAwareTestData")
    }

    @Test
    fun verifyDataRead() {
        val testFile = File(testDataFolder, FILE_NAME)

        FileWriter(testFile).use {
            it.write(TEST_DATA)
            it.flush()
        }
        val target = UploadAwareFileRequestBody(
            testFile.absolutePath,
            FILE_NAME,
            MimeType.TEXT_PLAIN.toString().toMediaType(),
            emptyList()
        )
        val sink = mockk<BufferedSink>()
        val buffer = Buffer()

        every { sink.flush() } returns Unit
        every { sink.buffer } returns buffer

        target.writeTo(sink)
        assertThat(buffer.size, `is`(SEGMENT_SIZE))
        assertThat(buffer.snapshot().string(StandardCharsets.UTF_8), `is`(TEST_DATA))
        assertThat(
            buffer.isOpen,
            `is`(true)
        )   // Verify the buffer is not closed by RequestBody, as it should be closed by OkHttp only.
    }

    @Test
    fun verifyDataReadWithCallbacks() {
        val testFile = File(testDataFolder, FILE_NAME)

        FileWriter(testFile).use {
            it.write(TEST_DATA)
            it.write(TEST_DATA)
            it.write(TEST_DATA)
            it.write(TEST_DATA)
            it.flush()
        }
        val fileSize = SEGMENT_SIZE * 4

        val callback = mockk<FileTransferProgressCallback>()
        justRun { callback(any(), any()) }

        val target = UploadAwareFileRequestBody(
            testFile.absolutePath,
            FILE_NAME,
            MimeType.TEXT_PLAIN.toString().toMediaType(),
            listOf(callback)
        )
        val sink = mockk<BufferedSink>()
        val buffer = Buffer()

        justRun { sink.flush() }
        every { sink.buffer } returns buffer

        target.writeTo(sink)
        assertThat(buffer.size, `is`(fileSize))
        assertThat(buffer.snapshot().string(StandardCharsets.UTF_8), `is`(TEST_DATA.repeat(4)))
        assertThat(
            buffer.isOpen,
            `is`(true)
        )   // Verify the buffer is not closed by RequestBody, as it should be closed by OkHttp only.
        target.onRequestComplete(true)
        val fileInfo =
            FileTransferInfo(testFile.absolutePath, FILE_NAME, MimeType.TEXT_PLAIN, true, fileSize)
        verifyOrder {
            callback(fileInfo, Resource.Loading(SEGMENT_SIZE))
            callback(fileInfo, Resource.Loading(SEGMENT_SIZE * 2))
            callback(fileInfo, Resource.Loading(SEGMENT_SIZE * 3))
            callback(fileInfo, Resource.Success(SEGMENT_SIZE * 4))
        }
    }

    @Test
    fun verifyDataReadCallbacksWithFileReadException() {
        val testFile = File(testDataFolder, FILE_NAME)

        val callback = mockk<FileTransferProgressCallback>()
        val resourceCapture = slot<Resource<Long>>()
        justRun { callback(any(), capture(resourceCapture)) }

        val target = UploadAwareFileRequestBody(
            testFile.absolutePath,
            FILE_NAME,
            MimeType.TEXT_PLAIN.toString().toMediaType(),
            listOf(callback)
        )
        val sink = mockk<BufferedSink>()
        val buffer = Buffer()

        justRun { sink.flush() }
        every { sink.buffer } returns buffer

        target.writeTo(sink)
        assertThat(buffer.size, `is`(0))
        assertThat(buffer.snapshot().string(StandardCharsets.UTF_8), `is`(""))
        assertThat(
            buffer.isOpen,
            `is`(true)
        )   // Verify the buffer is not closed by RequestBody, as it should be closed by OkHttp only.

        val fileInfo =
            FileTransferInfo(testFile.absolutePath, FILE_NAME, MimeType.TEXT_PLAIN, true, 0)
        verifyOrder {
            callback(fileInfo, any())
        }

        // Due to some issue with FileNotFoundException's equal function,
        // we have to capture the exception and manually assert the error message.
        assertThat(resourceCapture.captured, `is`(instanceOf(Resource.Error::class.java)))
        val exception = (resourceCapture.captured as Resource.Error<Long>).error
        assertThat(exception, `is`(instanceOf(FileNotFoundException::class.java)))
        assertThat(exception?.message, `is`("${testFile.absolutePath}: open failed: ENOENT (No such file or directory)"))
    }
}