package com.accelerator.network.core.common

/**
 * Class for holding individual list node.
 */
internal data class LinkedListNode<DATA>(val data: DATA, var next: LinkedListNode<DATA>? = null)

/**
 * A basic implementation of LinkedList for kotlin MultiPlatform.
 * Currently platform like iOS and JavaScript, doesn't provide implementation of LinkedList.
 * Hence created a bare minimum implementation of LinkedList for our use case.
 */
class LinkedList<DATA> {

    private var head: LinkedListNode<DATA>? = null
    private var tail: LinkedListNode<DATA>? = null

    val first: DATA?
        get() = head?.data

    /**
     * Remove the first element from the list
     *
     * @return [DATA] of first node if any, null otherwise.
     */
    fun removeFirst(): DATA? {
        if (head === tail) {
            head = null
            tail = null
            return null
        }
        val currentHead = head
        head = head?.next
        return currentHead?.data
    }

    /**
     * Add a data into the LinkedList.
     *
     * @param data [DATA] to be added in the list.
     * @return current [LinkedList] instance for BuilderPattern.
     */
    fun add(data: DATA) = apply {
        val node = LinkedListNode(data)
        if (head === null) {
            head = node
        }

        tail?.let { it.next = node }
        tail = node
    }
}
