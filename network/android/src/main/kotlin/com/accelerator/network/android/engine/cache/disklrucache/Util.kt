package com.accelerator.network.android.engine.cache.disklrucache

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.Reader
import java.io.StringWriter

/** class with helping functions for file operations
 * inspired by https://github.com/JakeWharton/DiskLruCache
 */

/**
 * Reads the [Reader] stream into a [String]
 *
 * @param readerStream [Reader] stream from which the data needs to be read.
 * @return [String] containing the whole data read from [readerStream].
 * @throws IOException if there are any problem while reading the file.
 */
@Throws(IOException::class)
internal fun readFully(readerStream: Reader) = readerStream.use { reader ->
    val writer = StringWriter()
    val buffer = CharArray(1024)
    var count: Int
    while (reader.read(buffer).also { count = it } != -1) {
        writer.write(buffer, 0, count)
    }
    return@use writer.toString()
}

/**
 * Deletes the contents of [dir].
 *
 * @param dir [File] or directory which needs to be deleted.
 * @throws DiskLruCacheException if any file could not be deleted, or if [dir] is not a readable directory.
 */
@Throws(DiskLruCacheException::class)
internal fun deleteContents(dir: File) {
    val files = dir.listFiles() ?: throw IOException("not a readable directory: $dir")
    for (file in files) {
        // When we have a directory, Android doesn't allow us to delete the directory,
        // Hence delete the content of the directory first.
        if (file.isDirectory) {
            deleteContents(file)
        }
        if (!file.delete()) {
            throw DiskLruCacheException(
                ErrorCode.DELETE_FILE_FAILED,
                "failed to delete file: $file"
            )
        }
    }
}

/**
 * Tries to close the [Closeable] without throwing any exceptions except [RuntimeException]
 *
 * @param closeable [Closeable] which needs to be closed.
 * @throws RuntimeException if any thrown while closing the [Closeable]
 */
@Throws(RuntimeException::class)
internal fun closeQuietly(closeable: Closeable?) {
    try {
        closeable?.close()
    } catch (rethrown: RuntimeException) {
        // Allow only RuntimeException to be thrown for quite close.
        throw rethrown
    } catch (ignored: Exception) {
        // NO-OP. Suppress other exceptions.
    }
}
