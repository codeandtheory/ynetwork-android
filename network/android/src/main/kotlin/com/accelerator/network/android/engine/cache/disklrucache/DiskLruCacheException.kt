package com.accelerator.network.android.engine.cache.disklrucache

/**
 * Error code representing different cases when exception is thrown during DiskLruCache operations
 *
 * @property type type of error
 * @constructor Create empty Error code
 */
enum class ErrorCode(val type: Int) {
    INVALID_JOURNAL_LINE(1),
    NO_VALUE_AT_INDEX(2),
    DELETE_FILE_FAILED(3),
    UNEXPECTED_JOURNAL_LINE(4),
    RENAME_FILE_FAILED(5),
    LINEREADER_CLOSED(6),
    UNEXPECTED_JOURNAL_HEADER(7),
}

/**
 * Custom exception class for DiskLruCache operations
 *
 * @param errorCode representing integer value of the ErrorCode.
 * @param errorMessage information representation of what the errorCode code meant.
 */
data class DiskLruCacheException(val errorCode: ErrorCode, val errorMessage: String? = null) :
    Exception("${errorCode.type} - $errorMessage")