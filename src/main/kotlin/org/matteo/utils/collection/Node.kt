package org.matteo.utils.collection

import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/05/19
 */
open class Node<K : Comparable<K>, V> @JvmOverloads constructor(
    val key: K? = null,
    var value: V? = null,
    private val comparator: Comparator<K?> = NullSafeComparator(),
    var parent: Node<K, V>? = null,
    private var children: TreeMap<K?, Node<K, V>> = TreeMap(comparator)
) {

    val isEmpty: Boolean
        get() = key == null

    val root: Node<K, V>
        get() = parent?.root ?: this

    val branch: Node<K, V>
        get() = getBranch(root)

    val firstChild: Node<K, V>?
        get() = if (!children.isEmpty()) children.firstEntry().value else null

    val childrenKeyList: List<K>
        get() = ArrayList(children.keys)

    val childrenList: List<Node<K, V>>
        get() = ArrayList(children.values)

    val leaves: List<Node<K, V>>
        get() = getLeaves(ArrayList())

    val leafMap: Map<K?, Node<K, V>>
        get() = getLeafMap(TreeMap())

    val childCount: Int
        get() = children.size

    val position: Int
        get() = parent?.let { Collections.binarySearch(it.childrenKeyList, key) } ?: 0

    val isRoot: Boolean
        get() = parent == null

    val isLeaf: Boolean
        get() = children.isEmpty()

    constructor(node: Node<K, V>) : this(node.key, node.value, node.comparator)

    private fun putNode(child: Node<K, V>) {
        children[child.key] = child
    }

    private fun putNodes(nodes: Collection<Node<K, V>>) {
        for (child in nodes) {
            children[child.key] = child
        }
    }

    private fun put(child: Node<K, V>): Node<K, V> {
        child.parent = this
        putNode(child)
        return child
    }

    private fun addNode(child: Node<K, V>): Node<K, V> {
        var node = getChild(child)
        if (node == null) {
            node = put(child)
        }
        return node
    }

    fun add(child: Node<K, V>): Node<K, V> {
        val node = addNode(child)
        for (c in child.getChildren()) {
            node.add(c)
        }
        return node
    }

    fun addCollapseEmpty(child: Node<K, V>): Node<K, V> {
        val node = if (!child.isEmpty) addNode(child) else this
        for (c in child.getChildren()) {
            node.addCollapseEmpty(c)
        }
        return node
    }

    fun addSkipEmpty(child: Node<K, V>): Node<K, V>? {
        return if (!child.isEmpty) {
            val node = addNode(child)
            for (c in child.getChildren()) {
                node.addSkipEmpty(c)
            }
            node
        } else {
            null
        }
    }

    fun addTo(parent: Node<K, V>): Node<K, V> = parent.also { it.add(this) }

    fun addToCollapseEmpty(parent: Node<K, V>): Node<K, V> = parent.also { it.addCollapseEmpty(this) }

    fun addToSkipEmpty(parent: Node<K, V>): Node<K, V> = parent.also { it.addSkipEmpty(this) }

    fun addAll(children: Collection<Node<K, V>>): Node<K, V> {
        for (node in children) {
            val child = getChild(node)
            child?.addAll(node.getChildren()) ?: put(node)
        }
        return this
    }

    fun addSibling(node: Node<K, V>): Node<K, V> = checkParent().add(node)

    private fun checkParent(): Node<K, V> {
        val par: Node<K, V> = parent ?: Node()
        parent = par
        return par
    }

    fun addSiblingTo(node: Node<K, V>): Node<K, V> = node.also { it.addSibling(this) }

    fun replace(child: Node<K, V>): Node<K, V> = remove(child).put(child)

    fun replaceCollapseEmpty(child: Node<K, V>): Node<K, V> {
        val node = if (!child.isEmpty) replace(child) else remove(child)
        for (c in child.getChildren()) {
            node.replaceCollapseEmpty(c)
        }
        return node
    }

    fun replaceSkipEmpty(child: Node<K, V>): Node<K, V> {
        return if (!child.isEmpty) {
            val node = replace(child)
            for (c in child.getChildren()) {
                node.replaceSkipEmpty(c)
            }
            node
        } else {
            Node()
        }
    }

    fun replaceTo(parent: Node<K, V>): Node<K, V> = parent.also { it.replace(this) }

    fun replaceToCollapseEmpty(parent: Node<K, V>): Node<K, V> = parent.also { it.replaceCollapseEmpty(this) }

    fun replaceToSkipEmpty(parent: Node<K, V>): Node<K, V> = parent.also { it.replaceSkipEmpty(this) }

    fun replaceAll(children: Collection<Node<K, V>>): Node<K, V> {
        for (child in children) {
            replace(child)
        }
        return this
    }

    fun replaceSibling(node: Node<K, V>): Node<K, V> = checkParent().replace(node)

    fun replaceSiblingTo(node: Node<K, V>): Node<K, V> = node.also { it.replaceSibling(this) }

    fun remove(key: K?): Node<K, V> {
        val child = getChild(key)
        if (child != null) {
            child.parent = null
            children.remove(key)
        }
        return this
    }

    fun remove(child: Node<K, V>): Node<K, V> = remove(child.key)

    fun removeBranch() {
        val parent = parent
        if (children.isEmpty() && parent != null) {
            val node = parent
            node.remove(this)
            node.removeBranch()
        }
    }

    fun removeAll(children: Collection<Node<K, V>>): Node<K, V> {
        for (child in children) {
            remove(child)
        }
        return this
    }

    fun joinChildTree(childRoot: Node<K, V>): Node<K, V> {
        for (node in leaves) {
            val child = childRoot.getChild(node)
            if (child != null) {
                node.parent?.replace(child)
            }
        }
        return this
    }

    fun intersectChildTree(childRoot: Node<K, V>): Node<K, V> {
        for (leaf in leaves) {
            val child = childRoot.getChild(leaf)
            if (child != null) {
                leaf.parent?.replace(child)
            } else {
                leaf.removeBranch()
            }
        }
        return this
    }

    fun merge(root: Node<K, V>): Node<K, V> {
        for (otherChild in root.getChildren()) {
            val child = getChild(otherChild)
            child?.merge(otherChild) ?: add(otherChild)
        }
        return this
    }

    fun subTree(from: Node<K, V>): Node<K, V> = cloneNode().also {
        it.children.putAll(children.tailMap(from.key))
    }

    fun subTree(from: Node<K, V>, to: Node<K, V>): Node<K, V> = cloneNode().also {
        it.children.putAll(children.subMap(from.key, true, to.key, true))
    }

    fun cloneNode(): Node<K, V> = Node(this)

    fun subTree(from: Int, size: Int): Node<K, V> =
        cloneNode().also { it.putNodes(childrenList.subList(from, from + size)) }

    fun filteredTree(predicate: Predicate<Node<K, V>>): Node<K, V> = filter(this, this, cloneNode(), predicate)

    private fun filter(
        ancestor: Node<K, V>,
        current: Node<K, V>,
        root: Node<K, V>,
        predicate: Predicate<Node<K, V>>
    ): Node<K, V> {
        for (node in current.getChildren()) {
            if (predicate.test(node)) {
                root.add(node.getBranch(ancestor))
            }
        }
        for (child in current.getChildren()) {
            child.filter(ancestor, child, root, predicate)
        }
        return root
    }

    fun find(key: K?): Node<K, V> = find(key, this, cloneNode(), { true })

    fun find(key: K?, predicate: Predicate<Node<K, V>>): Node<K, V> = find(key, this, cloneNode(), predicate)

    private fun find(key: K?, current: Node<K, V>, root: Node<K, V>, predicate: Predicate<Node<K, V>>): Node<K, V> {
        val found = current.getChild(key)
        if (found != null && predicate.test(found)) {
            root.add(found.branch)
        }
        for (child in current.getChildren()) {
            child.find(key, child, root, predicate)
        }
        return root
    }

    fun cloneTree(): Node<K, V> {
        val root = cloneNode()
        for (child in children.values) {
            root.put(child.cloneTree())
        }
        return root
    }

    fun traverseByDepthTopDown(consumer: Consumer<Node<K, V>>) {
        consumer.accept(this)
        for (child in children.values) {
            child.traverseByDepthTopDown(consumer)
        }
    }

    fun traverseByDepthBottomUp(consumer: Consumer<Node<K, V>>) {
        for (child in children.values) {
            child.traverseByDepthBottomUp(consumer)
        }
        consumer.accept(this)
    }

    fun traverseByBreadthTopDown(consumer: Consumer<Node<K, V>>) {
        consumer.accept(this)
        topDown(children.values, consumer)
    }

    private fun topDown(children: Collection<Node<K, V>>, consumer: Consumer<Node<K, V>>) {
        for (child in children) {
            consumer.accept(child)
        }
        for (child in children) {
            topDown(child.getChildren(), consumer)
        }
    }

    fun traverseByBreadthBottomUp(consumer: Consumer<Node<K, V>>) {
        bottomUp(children.values, consumer)
        consumer.accept(this)
    }

    private fun bottomUp(children: Collection<Node<K, V>>, consumer: Consumer<Node<K, V>>) {
        for (child in children) {
            bottomUp(child.getChildren(), consumer)
        }
        for (child in children) {
            consumer.accept(child)
        }
    }

    fun getBranch(ancestor: Node<K, V>?): Node<K, V> {
        var root = cloneNode()
        root.children = children
        var node = parent
        while (node != null && node != ancestor) {
            root = root.replaceTo(node.cloneNode())
            node = node.parent
        }
        return root
    }

    operator fun contains(child: Node<K, V>): Boolean = children.containsKey(child.key)

    fun containsKey(key: K?): Boolean = children.containsKey(key)

    fun getChild(key: K?): Node<K, V>? = children[key]

    fun getChild(child: Node<K, V>): Node<K, V>? = children[child.key]

    fun getChildren(): Collection<Node<K, V>> = children.values

    private fun getLeaves(leaves: MutableList<Node<K, V>>): List<Node<K, V>> {
        if (isLeaf) {
            leaves.add(this)
        } else {
            for (child in children.values) {
                child.getLeaves(leaves)
            }
        }
        return leaves
    }

    private fun getLeafMap(leafMap: MutableMap<K?, Node<K, V>>): Map<K?, Node<K, V>> {
        if (isLeaf) {
            leafMap[key] = this
        } else {
            for (child in children.values) {
                child.getLeafMap(leafMap)
            }
        }
        return leafMap
    }

    fun getAncestor(filter: Predicate<Node<K, V>>): Node<K, V>? {
        var node: Node<K, V>? = this
        while (node != null) {
            if (filter.test(node)) {
                break
            }
            node = node.parent
        }
        return node
    }

    fun size(): Int = childCount + children.values.map { it.size() }.sum()

    fun clear() {
        parent = null
        children.clear()
    }

    fun clearBranch() {
        for (child in children.values) {
            child.destroy()
        }
    }

    fun destroy() {
        clearBranch()
        clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node<*, *>

        if (key != other.key) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        val child = if (children.isEmpty()) "" else " -> ${children.values}"
        return "$key$child"
    }

}

class NullSafeComparator<T : Comparable<T>> : Comparator<T?> {

    override fun compare(t1: T?, t2: T?): Int {
        if (t1 != null && t2 != null) {
            return t1.compareTo(t2)
        } else if (t1 != null) {
            return 1
        } else if (t2 != null) {
            return -1
        }
        return 0
    }

}
