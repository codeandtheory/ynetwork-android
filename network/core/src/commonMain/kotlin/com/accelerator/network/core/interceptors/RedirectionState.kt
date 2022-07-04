package com.accelerator.network.core.interceptors

/**
 * Class to provide the state of redirection operation.
 */
sealed class RedirectionState<DATA> {

    /**
     * This state indicates that the interceptor has allowed the redirection and
     * the redirection request needs to be performed with given [data].
     *
     * @param data containing data for redirection.
     */
    data class Allowed<DATA>(val data: DATA) : RedirectionState<DATA>()

    /**
     * This state indicates that the interceptor doesn't want to perform any redirection
     * and would like to cancel the current redirection operation.
     */
    class Cancel<DATA> : RedirectionState<DATA>()

    /**
     * This state indicates that the interceptor doesn't want to decide on whether to perform/cancel
     * the redirection instead it want other interceptors (if any) to make that decision.
     * This state can be interpreted as Allowed with other interceptor's permission.
     *
     * This state can be used by the interceptors to observe the redirection call without modifying
     * the redirect data. (e.g. logging interceptors, analytics interceptors where they just want to
     * observer the redirection operation and doesn't want to cancel/allow redirection operation.)
     */
    class NoOp<DATA> : RedirectionState<DATA>()
}
