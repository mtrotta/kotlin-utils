package org.matteo.utils.collection

import java.util.*

open class PriorityHashSet<T : Comparable<T>> : MutableSet<T> {

    private val map = mutableMapOf<T, T>()
    private val comparator: Comparator<T>

    constructor() {
        this.comparator = Comparator.naturalOrder()
    }

    constructor(comparator: Comparator<T>) {
        this.comparator = comparator
    }

    override val size: Int
        get() = map.size

    override fun iterator(): MutableIterator<T> = map.values.iterator()

    override fun remove(element: T): Boolean = map.keys.remove(element)

    override fun removeAll(elements: Collection<T>): Boolean = map.keys.removeAll(elements)

    override fun retainAll(elements: Collection<T>): Boolean = map.keys.retainAll(elements)

    override fun contains(element: T): Boolean = map.keys.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = map.keys.containsAll(elements)

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun add(element: T): Boolean {
        val existing = map[element]
        if (existing == null || comparator.compare(element, existing) < 0) {
            map[element] = element
            return true
        }
        return false
    }

    override fun addAll(elements: Collection<T>): Boolean = elements
        .map { add(it) }
        .reduce { acc, value -> value or acc }

    override fun clear() = map.clear()

}
