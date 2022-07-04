package com.accelerator.network.android.engine.cache.disklrucache

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private const val CR = '\r'.code.toByte()
private const val LF = '\n'.code.toByte()

/**
 * Buffers input from an [InputStream] for reading lines.
 *
 *
 * This class is used for buffered reading of lines. For purposes of this class, a line ends
 * with "\n" or "\r\n". End of input is reported by throwing `EOFException`. Unterminated
 * line at end of input is invalid and will be ignored, the caller may use `hasUnterminatedLine()` to detect it after catching the `EOFException`.
 *
 *
 * This class is intended for reading input that strictly consists of lines, such as line-based
 * cache entries or cache journal. Unlike the [java.io.BufferedReader] which in conjunction
 * with [java.io.InputStreamReader] provides similar functionality, this class uses different
 * end-of-input reporting and a more restrictive definition of a line.
 *
 *
 * This class supports only charsets that encode '\r' and '\n' as a single byte with value 13
 * and 10, respectively, and the representation of no other character contains these values.
 * We currently check in constructor that the charset is one of US-ASCII, UTF-8 and ISO-8859-1.
 * The default charset is US_ASCII.
 *
 * @constructor a new [StrictLineReader] with the specified charset and the default capacity.
 *
 * @param inputStream the [InputStream] to read data from.
 * @param charset the charset used to decode data. Only US-ASCII, UTF-8 and ISO-8859-1 are supported.
 * @param capacity the capacity of the buffer.
 *
 * @throws IllegalArgumentException if capacity is negative or zero or the specified [charset] is not supported.
 */
internal class StrictLineReader(
    private val inputStream: InputStream,
    private val charset: Charset,
    capacity: Int = 8192  // 8192 bytes = 8 KB
) : Closeable {

    /**
     * Buffered data is stored in [bufferByteArray]. As long as no exception occurs, 0 <= pos <= end
     * and the data in the range [pos, end] is buffered for reading. At end of input, if there is
     * an unterminated line, we set end == -1, otherwise end == pos. If the underlying
     * [InputStream] throws an [IOException], end may remain as either pos or -1.
     */
    private var bufferByteArray: ByteArray?
    private var pos = 0
    private var end = 0

    init {
        require(capacity >= 0) { "capacity <= 0" }
        require(charset == StandardCharsets.US_ASCII) { "Unsupported encoding" }
        bufferByteArray = ByteArray(capacity)
    }

    /**
     * Closes the reader by closing the underlying [InputStream] and marking this reader as closed.
     *
     * @throws IOException for errors when closing the underlying `InputStream`.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun close(): Unit = synchronized(inputStream) {
        bufferByteArray?.let {
            bufferByteArray = null
            inputStream.close()
        }
    }

    /**
     * Reads the next line. A line ends with `"\n"` or `"\r\n"`,
     * this end of line marker is not included in the result.
     *
     * @return the next line from the input.
     * @throws IOException for underlying [InputStream] errors.
     * @throws EOFException for the end of source stream.
     */
    @Synchronized
    @Throws(DiskLruCacheException::class)
    fun readLine(): String {
        synchronized(inputStream) {
            val buffer = bufferByteArray
                ?: throw DiskLruCacheException(ErrorCode.LINEREADER_CLOSED, "LineReader is closed")

            // Read more data if we are at the end of the buffered data.
            // Though it's an error to read after an exception, we will let {@code fillBuf()}
            // throw again if that happens; thus we need to handle end == -1 as well as end == pos.
            if (pos >= end) {
                fillBuf()
            }
            // Try to find LF in the buffered data and return the line if successful.
            for (i in pos until end) {
                if (buffer[i] == LF) {
                    val lineEnd = if (i != pos && buffer[i - 1] == CR) i - 1 else i
                    val res = String(buffer, pos, lineEnd - pos, charset)
                    pos = i + 1
                    return res
                }
            }

            // Let's anticipate up to 80 characters on top of those already read.
            val out = object : ByteArrayOutputStream(end - pos + 80) {
                override fun toString(): String = try {
                    val length = if (count > 0 && buf[count - 1] == CR) count - 1 else count
                    String(buf, 0, length, charset)
                } catch (e: UnsupportedEncodingException) {
                    throw AssertionError(e) // Since we control the charset this will never happen.
                }
            }
            while (true) {
                out.write(buffer, pos, end - pos)
                // Mark unterminated line in case fillBuf throws EOFException or IOException.
                end = -1
                fillBuf()
                // Try to find LF in the buffered data and return the line if successful.
                for (i in pos until end) {
                    if (buffer[i] == LF) {
                        if (i != pos) {
                            out.write(buffer, pos, i - pos)
                        }
                        pos = i + 1
                        return out.toString()
                    }
                }
            }
        }
    }

    fun hasUnterminatedLine() = end == -1

    /**
     * Reads new input data into the buffer. Call only with pos == end or end == -1,
     * depending on the desired outcome if the function throws.
     *
     * @throws IOException in case there is a problem while reading the data.
     */
    @Throws(IOException::class)
    private fun fillBuf() {
        val result = inputStream.read(bufferByteArray, 0, bufferByteArray?.size ?: 0)
        if (result == -1) {
            throw EOFException()
        }
        pos = 0
        end = result
    }
}