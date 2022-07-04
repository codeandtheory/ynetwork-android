package com.yml.chat.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val cursorIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                result = if (cursorIndex < -1) null else it.getString(cursorIndex)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}