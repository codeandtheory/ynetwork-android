package co.yml.network.core.common

/**
 * Class representing an individual Node in [TreeMap].
 */
data class TreeEntryNode<KEY, VALUE>(
    var key: KEY,
    var value: VALUE,
    var left: TreeEntryNode<KEY, VALUE>? = null,
    var right: TreeEntryNode<KEY, VALUE>? = null
)

/**
 * A basic implementation of TreeMap for kotlin multiplatform.
 * Currently platform like iOS and JavaScript, doesn't provide implementation of LinkedList.
 * Hence created a bare minimum implementation of TreeMap for our use case.
 */
class TreeMap<KEY, VALUE>(private val comparator: Comparator<KEY>) {

    private var root: TreeEntryNode<KEY, VALUE>? = null

    /**
     * Retrieve the value for given [key]
     *
     * @param key [KEY] for which data needs to be fetched.
     * @return [VALUE] for the specified key (if any), null otherwise.
     */
    fun get(key: KEY): VALUE? = getRecursive(root, key)

    fun getOrPut(key: KEY, defaultValue: () -> VALUE): VALUE {
        val prevValue = get(key)
        if (prevValue != null) {
            return prevValue
        }
        val value = defaultValue()
        root = addRecursive(root, key, value)
        return value
    }

    /**
     * Remove the data represented by [key] from the [TreeMap].
     *
     * @param key [KEY] for which the data needs to be removed.
     */
    fun remove(key: KEY) {
        root = deleteRecursive(root, key)
    }

    private fun getRecursive(currentNode: TreeEntryNode<KEY, VALUE>?, key: KEY): VALUE? {
        return currentNode?.let {
            val compareResult = comparator.compare(it.key, key)
            when {
                compareResult < 0 -> getRecursive(it.left, key)
                compareResult > 0 -> getRecursive(it.right, key)
                else -> return it.value
            }
        }
    }

    private fun addRecursive(
        currentNode: TreeEntryNode<KEY, VALUE>?,
        key: KEY,
        value: VALUE
    ): TreeEntryNode<KEY, VALUE> {
        return currentNode?.let {
            val compareResult = comparator.compare(it.key, key)
            when {
                compareResult < 0 -> it.left = addRecursive(it.left, key, value)
                compareResult > 0 -> it.right = addRecursive(it.right, key, value)
                else -> it.value = value
            }
            return it
        } ?: TreeEntryNode(key, value)
    }

    // Ref: https://github.com/eugenp/tutorials/blob/master/data-structures/src/main/java/com/baeldung/tree/BinaryTree.java#L64
    private fun deleteRecursive(
        current: TreeEntryNode<KEY, VALUE>?,
        key: KEY
    ): TreeEntryNode<KEY, VALUE>? {
        return current?.let {
            val compareResult = comparator.compare(it.key, key)
            when {
                compareResult < 0 -> current.left = deleteRecursive(current.left, key)
                compareResult > 0 -> current.right = deleteRecursive(current.right, key)
                else -> {
                    // Case 1: no children
                    if (current.left == null && current.right == null) {
                        return null
                    }

                    // Case 2: only 1 child
                    if (current.right == null) {
                        return current.left
                    }
                    if (current.left == null) {
                        return current.right
                    }

                    // Case 3: 2 children
                    findSmallestValue(current.right)?.let { smallestValue ->
                        current.value = smallestValue.value
                        current.key = smallestValue.key
                        current.right = deleteRecursive(current.right, smallestValue.key)
                    }
                }
            }
            return current
        }
    }

    private fun findSmallestValue(root: TreeEntryNode<KEY, VALUE>?): TreeEntryNode<KEY, VALUE>? =
        root?.left?.let { findSmallestValue(it) } ?: root
}