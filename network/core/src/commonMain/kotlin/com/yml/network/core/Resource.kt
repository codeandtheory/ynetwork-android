package com.yml.network.core

/**
 * Resource class to represent current status of the task/operation.
 */
sealed class Resource<DATA> {
    /**
     * Resource class representing the task is successful.
     *
     * @param data the data generated/captured during the task.
     */
    data class Success<DATA>(val data: DATA) : Resource<DATA>()

    /**
     * Resource class representing the task is still ongoing.
     *
     * @param data the data generated/captured during the task.
     * @param error the error/exception generated during the task.
     */
    data class Loading<DATA>(val data: DATA? = null, val error: Exception? = null) : Resource<DATA>()

    /**
     * Resource class representing the task is failed.
     *
     * @param error the error/exception generated during the task.
     */
    data class Error<DATA>(val error: Exception? = null) : Resource<DATA>()
}
