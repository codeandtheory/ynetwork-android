package com.accelerator.network.android.engine.network.body

import androidx.annotation.VisibleForTesting
import com.yml.network.core.MimeType
import com.yml.network.core.Resource
import com.yml.network.core.request.FileTransferInfo
import com.yml.network.core.request.FileTransferProgressCallback
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File

// A large file will be send in the chunks. The Segment Size indicate the maximum size of single chunk.
@VisibleForTesting
const val SEGMENT_SIZE = 8192.toLong()   // okio.Segment.SIZE => 8KB

/**
 * Class which is aware of the upload progress.
 *
 * @see <a href="https://stackoverflow.com/a/26376724">https://stackoverflow.com/a/26376724</a>
 */
class UploadAwareFileRequestBody(
    filePath: String,
    private val fileName: String?,
    private val mediaType: MediaType?,
    private val callbackList: List<FileTransferProgressCallback>
) : RequestBody() {
    private val file = File(filePath)

    override fun contentType(): MediaType? = mediaType

    override fun writeTo(sink: BufferedSink) {
        val fileSizeInBytes = file.length()
        val fileInfo = FileTransferInfo(
            filePath = file.absolutePath,
            fileName = fileName,
            mimeType = mediaType?.toString()?.let { MimeType(it) },
            isUpload = true,
            fileSizeInBytes = fileSizeInBytes
        )
        try {
            file.source().use { source ->
                var totalReadBytes: Long = 0
                var read: Long
                try {
                    while (source.read(sink.buffer, SEGMENT_SIZE).also { read = it } != -1L) {
                        totalReadBytes += read
                        sink.flush()
                        if (callbackList.isNotEmpty()) {
                            val resource = Resource.Loading(totalReadBytes)
                            callbackList.forEach { it(fileInfo, resource) }
                        }
                    }
                } catch (error: Exception) {
                    callbackList.forEach { it(fileInfo, Resource.Error(error)) }
                }
            }
        } catch (error: Exception) {
            callbackList.forEach { it(fileInfo, Resource.Error(error)) }
        }
    }

    fun onRequestComplete(isSuccess: Boolean) {
        val fileSizeInBytes = file.length()
        val fileInfo = FileTransferInfo(
            filePath = file.absolutePath,
            fileName = fileName,
            mimeType = mediaType?.toString()?.let { MimeType(it) },
            isUpload = true,
            fileSizeInBytes = fileSizeInBytes
        )
        val resource = if (isSuccess) Resource.Success(fileSizeInBytes) else Resource.Error()
        callbackList.forEach { it(fileInfo, resource) }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UploadAwareFileRequestBody) {
            return false
        }
        return file == other.file && mediaType == other.mediaType
    }

    override fun hashCode(): Int {
        var hashcode = file.hashCode()
        hashcode = (hashcode * 37) + mediaType.hashCode()
        return hashcode
    }

    override fun toString(): String {
        return "${UploadAwareFileRequestBody::class.simpleName}{file = $file, mediaType: $mediaType}"
    }
}
