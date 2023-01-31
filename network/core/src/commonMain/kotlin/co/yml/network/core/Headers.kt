package co.yml.network.core

typealias HeaderEntry = Pair<String, String>

class Headers(existingHeaders: Headers? = null) {
    val size
        get() = headers.size

    private val headers: MutableList<HeaderEntry> = ArrayList()

    init {
        // Add all entries from previous/existing headers into the current header.
        existingHeaders?.let { append(it) }
    }

    /**
     * Add a header entry with given data
     *
     * @param headerName Name of the header
     * @param value Value for the given [headerName]
     *
     * @return [Headers]'s current instance for builder pattern.
     */
    fun add(headerName: String, value: String) = apply {
        headers.add(headerName.lowercase() to value)
    }

    /**
     * Append all entries from other headers into current instance.
     *
     * @param [headers] other [Headers] instance from which all entries needs to be appended.
     *
     * @return [Headers]'s current instance for builder pattern.
     */
    fun append(headers: Headers) = apply {
        headers.forEach { name, value -> this.headers.add(name to value) }
    }

    /**
     * Get the first header entry with the given [headerName]
     *
     * @param headerName name of the header that needs to be found.
     *
     * @return [String], if the header is present for [headerName]. null, otherwise.
     */
    fun get(headerName: String): String? =
        headerName.lowercase().let { headers.find { entry -> entry.first == it }?.second }

    /**
     * Get the last header entry with the given [headerName]
     *
     * @param headerName name of the header that needs to be found.
     *
     * @return [String], if the header is present for [headerName]. null, otherwise.
     */
    fun getLast(headerName: String): String? =
        headerName.lowercase().let { headers.findLast { entry -> entry.first == it }?.second }

    /**
     * Get all the header values for given [headerName]
     *
     * @param headerName name of the header
     *
     * @return [List] containing all the values belonging to [headerName]
     */
    fun getAll(headerName: String) = headerName.lowercase().let {
        headers.filter { entry -> entry.first == it }.map { entry -> entry.second }
    }

    fun keys() = HashSet(headers.map { it.first })

    /**
     * Remove the header entry for given [headerName] and [value].
     *
     * @param [headerName] Name of the header that needs to be removed.
     * @param [value] Value of the header that needs to be removed.
     *
     * @return true, when header entry is removed successfully. false, otherwise.
     */
    fun remove(headerName: String, value: String) =
        headers.remove(headerName.lowercase() to value)

    /**
     * Remove all header entries matching the given [headerName].
     *
     * @param [headerName] Name of the header, that needs to be removed.
     *
     * @return true, when at least one element is matched and removed. false, otherwise
     */
    fun removeAll(headerName: String) =
        headerName.lowercase().let { removeAll { headerName, _ -> headerName == it } }

    /**
     * Remove all header entries matched by the given [predicate]
     *
     * @param [predicate] Function to determine which entries to be remove. The entries for which this function returns true, would be removed.
     *
     * @return true, when at least one element is matched and removed. false, otherwise
     */
    fun removeAll(predicate: (headerName: String, value: String) -> Boolean) =
        headers.removeAll { predicate(it.first, it.second) }

    /**
     * Iterate through all the header entries
     *
     * @param [action] Function to execute custom logic for each entry.
     */
    fun forEach(action: (headerName: String, value: String) -> Unit) =
        headers.forEach { action(it.first, it.second) }

    override fun equals(other: Any?): Boolean {
        if (other !is Headers) {
            return false
        }
        return this.headers == other.headers
    }

    override fun hashCode(): Int = this.headers.hashCode()

    override fun toString(): String = this.headers.toString()
}