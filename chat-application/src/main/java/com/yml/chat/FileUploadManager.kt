package com.yml.chat

import com.accelerator.network.core.Resource
import com.accelerator.network.core.request.FileTransferInfo
import com.accelerator.network.core.request.FileTransferProgressCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferManager @Inject constructor() {

    val fileTransferCallback = mutableListOf<FileTransferProgressCallback>()

    fun onUpdate(fileInfo: FileTransferInfo, transferredBytesResource: Resource<Long>) {
        fileTransferCallback.forEach { it(fileInfo, transferredBytesResource) }
    }
}
