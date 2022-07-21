package com.yml.chat

import com.yml.network.core.Resource
import com.yml.network.core.request.FileTransferInfo
import com.yml.network.core.request.FileTransferProgressCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferManager @Inject constructor() {

    val fileTransferCallback = mutableListOf<FileTransferProgressCallback>()

    fun onUpdate(fileInfo: FileTransferInfo, transferredBytesResource: Resource<Long>) {
        fileTransferCallback.forEach { it(fileInfo, transferredBytesResource) }
    }
}
