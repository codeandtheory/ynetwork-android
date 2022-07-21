package com.yml.network.core.request

private const val SLASH_DELIMITER = "/"
private const val SYMBOL_AND = "&"
private const val SYMBOL_EQUAL = "="
private const val SYMBOL_TERNARY = "?"

/**
 * String representation of Query param regex.
 *
 * Breakdown:
 * (\?([\w-.%]+=[\w-.%]+)(&[\w-.%]+=[\w-.%]+)*)
 *  |-|-------| |-------|-|-------| |-------|
 *   ? Key     = Value  (&  Key    =  Value)*
 */
private const val queryParamRegexStr = "(\\?([\\w-.%]+=[\\w-.%]+)(&[\\w-.%]+=[\\w-.%]+)*)"

/**
 * Regex pattern to validate full url.
 *
 * Following is the breakdown of the regex:
 *
 * ^(http[s]?://)?([\w-]+(\.[\w-]+)+)(/[\w-.#%]+)* /?(#[\w-.%]+)?(\?([\w-.%]+=[\w-.%]+)(&[\w-.%]+=[\w-.%]+)*)?(#[\w-.%]+)?$
 *  |-----------| |-----------------------------|   |---------| |-------------------------------------------| |---------|
 *    http/https            Domain Name               Anchor                       Query params                 Anchor
 */
internal val fullURLRegexPattern =
    "^(http[s]?://)?([\\w-]+(\\.[\\w-]+)+)(/[\\w-.#%]+)*/?(#[\\w-.%]+)?$queryParamRegexStr?(#[\\w-.%]+)?\$".toRegex()

internal val queryParamsRegexPattern = "$queryParamRegexStr\$".toRegex()

/**
 * Class to store the request path data.
 * @param basePathSegment store absolute url
 * @param charset which needs to be used while building the url
 */
class RequestPath(basePathSegment: String = "", private val charset: String = "utf-8") {

    /**
     * Field to store base path for the URL .
     */
    var basePath: String? = null
        private set

    /**
     * Field to store path segment which is appended with base path for creating  URL .
     */
    private val pathSegment: StringBuilder = StringBuilder()

    /**
     * Field to store set of parameters attached to the end of a url.
     */
    var queryParams: MutableList<Pair<String, String>> = mutableListOf()
        private set

    init {
        if (basePathSegment.matches(fullURLRegexPattern)) {
            this.setBasePath(basePathSegment)
        } else {
            this.pathSegment.append(basePathSegment)
        }
    }

    /**
     * Set the base path for url.
     *
     * @param basePath base path of the url as string.
     *
     * @return [RequestPath]'s instance for builder pattern
     */
    fun setBasePath(basePath: String) = apply {
        var modifiedBasePath = basePath
        // Extract and remove query params from the base path.
        queryParamsRegexPattern.find(basePath)?.let { match ->
            val fullMatchedStr =
                match.groupValues.getOrNull(0) ?: return@let // Full matched string with `?` at start
            val keyValuePairStr = fullMatchedStr.removePrefix("?") // Query key-value pair data separated by `&`.
            val pairs = keyValuePairStr.split('&').mapNotNull { keyValuePair ->
                val pair = keyValuePair.split('=')
                val key = pair.getOrNull(0)
                val value = pair.getOrNull(1)
                if (key != null && value != null) {
                    return@mapNotNull key to decodeUrlData(value, charset)
                }
                return@mapNotNull null
            }
            // Add all the query params at the start of the list.
            queryParams.addAll(0, pairs)
            modifiedBasePath = modifiedBasePath.removeSuffix(fullMatchedStr)
        }
        this.basePath = modifiedBasePath
    }

    /**
     * Append the path segment with existing one.
     *
     * @param segments relative path for url
     *
     * @return [RequestPath]'s instance for builder pattern
     */
    fun appendPathSegment(vararg segments: String) = apply {
        segments.forEach { appendUrlPath(pathSegment, it) }
    }

    /**
     * Set the query parameters added with url.
     *
     * @param key holds value of name of key parameter
     * @param value holds value of key parameter
     *
     * @return [RequestPath]'s instance for builder pattern
     */
    fun addParams(key: String, value: String) = apply {
        this.queryParams.add(key to value)
    }

    /**
     * Remove specific query parameters added with url.
     *
     * @param key key of query parameters which will be removed.
     * @param value value of the query parameters which will be removed.
     *
     * @return Boolean. True, in case the query param is removed. False, otherwise
     */
    fun remove(key: String, value: String) =
        this.queryParams.remove(key to value)

    /**
     * Remove specific query parameters added with url.
     *
     * @param key key of query parameters which will be removed.
     *
     * @return Boolean. True, in case the query param is removed. False, otherwise
     */
    fun removeAll(key: String) = removeAll { entryKey, _ -> entryKey == key }

    /**
     * Remove query parameters added with url.
     *
     * @param predicate to match the query parameters which will be removed.
     *
     * @return Boolean. True, in case the query param is removed. False, otherwise
     */
    fun removeAll(predicate: (key: String, value: String) -> Boolean) =
        this.queryParams.removeAll { predicate(it.first, it.second) }

    /**
     * @return full path excluding query parameters from [RequestPath]'s instance
     */
    fun buildPathWithoutParams(): String {
        val url = StringBuilder()
        if (!basePath.isNullOrEmpty()) {
            url.append(this.basePath)
        }
        if (pathSegment.isNotEmpty()) {
            appendUrlPath(url, pathSegment.toString())
        }
        return url.toString()
    }

    override fun toString() = buildPath()

    /**
     * Validate RequestPath and return processed url as string
     *
     * @throws IllegalArgumentException for empty or null basePath or for any invalid url
     * @return url after adding base path,path segment and query parameter
     */
    fun build(): String {
        if (basePath.isNullOrEmpty()) {
            throw  IllegalArgumentException("Base path is not specified")
        }
        val path = buildPath()
        return if (path.matches(fullURLRegexPattern)) path else throw IllegalArgumentException("Invalid url")
    }

    /**
     * Removes the irrelevant slash from path segment and appends multiple segment with each other
     *
     * @param segment new path segment to be added
     *
     */
    private fun appendUrlPath(urlPath: StringBuilder, segment: String) {
        val isExistingPathHasTrailingSlash = urlPath.endsWith(SLASH_DELIMITER)
        val isNewPathHasForwardSlash = segment.startsWith(SLASH_DELIMITER)
        if (!isExistingPathHasTrailingSlash && !isNewPathHasForwardSlash) {
            // Both path doesn't have any slashes
            urlPath.append(SLASH_DELIMITER).append(segment)
        } else if (isExistingPathHasTrailingSlash && isNewPathHasForwardSlash) {
            // Both path has slash, so remove slash from new segment
            urlPath.append(segment.substring(1))
        } else {
            // One of the substring has a slash, which wouldn't result in conflict
            urlPath.append(segment)
        }
    }

    /**
     * Append query parameters to url obtained by adding base path and path segment
     *
     * @return url after adding base path,path segment and query parameter
     */
    private fun buildPath(): String {
        var url = buildPathWithoutParams()
        val queryParam = this.queryParams.joinToString(SYMBOL_AND) {
            it.first + SYMBOL_EQUAL + encodeUrlData(it.second, charset)
        }
        if (queryParam.isNotEmpty()) {
            url += SYMBOL_TERNARY + queryParam
        }
        return url
    }

}
