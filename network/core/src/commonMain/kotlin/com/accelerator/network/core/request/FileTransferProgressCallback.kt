package com.accelerator.network.core.request

import com.accelerator.network.core.MimeType
import com.accelerator.network.core.Resource

/**
 * Data class holding file transfer progress.
 *
 * @constructor
 * @param filePath Path of the file which can be used to uniquely identify a file
 * @param fileName original name of the file to show it to the user, as the application may have
 *                  copied the file in cache directory with different name to avoid name collision.
 * @param mimeType content type of the file
 * @param isUpload specify whether the file transfer is upload or a download.
 * @param fileSizeInBytes total size of the file in bytes.
 */
data class FileTransferInfo(val filePath: String,
                    val fileName: String?,
                    val mimeType: MimeType?,
                    val isUpload: Boolean,
                    val fileSizeInBytes: Long)

/**
 * Function for notifying the progress of file transfer (upload/download).
 */
typealias FileTransferProgressCallback = (fileInfo: FileTransferInfo, transferredBytesResource: Resource<Long>) -> Unit
