package com.accelerator.network.android.engine.cache.disklrucache

import com.accelerator.network.core.Headers
import java.io.InputStream

internal const val HEADER_SEPARATOR = ": "

internal fun streamToHeaders(inputStream: InputStream?): Headers {
    val headers = Headers()
    inputStream?.bufferedReader(Charsets.UTF_8)?.use {
        val headersCount = it.readLine()?.toInt() ?: 0
        for (index in 0 until headersCount) {
            it.readLine()?.let { line ->
                val separatorIndex = line.indexOf(HEADER_SEPARATOR)
                val key = if (separatorIndex <= 0) "" else line.substring(0, separatorIndex)
                val value = line.substring(startIndex = separatorIndex + HEADER_SEPARATOR.length)
                headers.add(key, value)
            }
        }
    }
    return headers
}

internal fun headersToString(headers: Headers?): String {
    val stringBuilder = StringBuilder()
    stringBuilder.append("${headers?.size ?: 0}\n")  // The output string consider a int value as a Char ASCII code, hence sending string value of int
    headers?.forEach { headerName, value ->
        stringBuilder.append("$headerName$HEADER_SEPARATOR$value\n")
    }
    return stringBuilder.toString()
}