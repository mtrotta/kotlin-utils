package org.matteo.utils.collection

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 03/05/12
 * Time: 16.35
 */
internal class NodeTest {

    private val filterN1 = { n: Node<String, NodeType> -> NodeType.N1 == n.value }
    private val filterN2 = { n: Node<String, NodeType> -> NodeType.N2 == n.value }

    internal enum class NodeType {
        N1,
        N2,
        N3,
        LEAF
    }

    private inner class Data {
        val root = Node<String, NodeType>()
        val child = Node<String, NodeType>("1_Child")
        val sameChild = Node<String, NodeType>("1_Child")
        val empty = Node<String, NodeType>()
        val leaf = Node("1_Leaf", NodeType.LEAF)

        val root2 = Node<String, NodeType>()
        val child2 = Node<String, NodeType>("2_Child")
        val sameChild2 = Node<String, NodeType>("2_Child")
        val leaf2 = Node<String, NodeType>("2_Leaf")
    }

    @Test
    fun testBase() {
        val data = Data()
        assertTrue(data.root.isEmpty)
        assertTrue(data.root.isRoot)
        assertTrue(data.empty.isEmpty)
    }

    @Test
    fun testNoValue() {
        val r = Node<String, String>()
        val c = Node<String, String>("data.child")
        r.add(c)
        assertEquals(1, r.size())
        assertNull(r.value)
        assertNull(c.value)
    }

    @Test
    fun testEquals() {
        val data = Data()
        assertEquals(data.root, data.root2)
        assertEquals(data.child, data.sameChild)
        assertEquals(data.child2, data.sameChild2)
        assertEquals(data.empty, data.empty)

        data.root.add(data.child)
        data.root2.add(data.sameChild)

        assertEquals(data.child, data.sameChild)

        data.root.destroy()
        data.root2.destroy()

        data.root.add(data.leaf).add(data.child)
        data.root2.add(data.leaf2).add(data.sameChild)

        assertNotEquals(data.child, data.sameChild)
    }

    @Test
    fun testPosition() {
        val data = Data()
        data.root.add(data.child)
        data.root.add(data.child2)
        assertEquals(0, data.child.position)
        assertEquals(1, data.child2.position)
    }

    @Test
    fun testReplace() {
        val data = Data()
        assertTrue(data.root.isLeaf)
        assertTrue(data.root.isRoot)

        data.root.replace(data.child)
        assertFalse(data.root.isLeaf)
        assertTrue(data.child.isLeaf)
        assertTrue(data.root.contains(data.child))
        assertTrue(data.root.containsKey(data.child.key))
        assertEquals(data.root, data.child.root)
        assertEquals(data.root, data.child.parent)
        assertEquals(data.child, data.root.getChild(data.child))
        assertEquals(data.child, data.root.firstChild)
        assertEquals(1, data.root.getChildren().size)
        assertEquals(1, data.root.childrenList.size)
        assertEquals(1, data.root.childCount)
        assertEquals(1, data.root.leaves.size)
        assertEquals(1, data.root.leafMap.size)
        assertEquals(1, data.root.size())

        data.child.replace(data.leaf)
        assertFalse(data.root.isLeaf)
        assertFalse(data.child.isLeaf)
        assertFalse(data.root.contains(data.leaf))
        assertTrue(data.child.contains(data.leaf))
        assertEquals(data.root, data.leaf.root)
        assertEquals(data.child, data.leaf.parent)
        assertEquals(data.leaf, data.child.getChild(data.leaf))
        assertEquals(1, data.root.childCount)
        assertEquals(1, data.root.leaves.size)
        assertEquals(2, data.root.size())

        data.root.destroy()
        assertEquals(0, data.root.size())
    }

    @Test
    fun testReplaceTo() {
        val data = Data()
        var ret = data.child.replaceTo(data.root)

        assertSame(data.root, ret)
        assertTrue(data.root.isRoot)
        assertFalse(data.root.isLeaf)
        assertTrue(data.child.isLeaf)
        assertFalse(data.child.isRoot)
        assertTrue(data.root.contains(data.child))

        data.empty.replaceToCollapseEmpty(data.child)
        assertTrue(data.child.isLeaf)

        ret = data.leaf.replaceTo(data.empty).replaceToSkipEmpty(data.child)
        assertSame(data.child, ret)
        assertTrue(data.child.isLeaf)

        ret = data.leaf.replaceTo(data.empty).replaceToCollapseEmpty(data.child)
        assertSame(data.child, ret)
        assertFalse(data.child.isLeaf)
    }

    @Test
    fun testAdd() {
        val data = Data()
        val ret = data.root.add(data.child)
        assertSame(data.child, ret)
        assertEquals(data.child, data.root.getChild(data.child))
        data.root.add(data.sameChild)
        assertSame(data.child, data.root.getChild(data.child))
        assertNotSame(data.sameChild, data.root.getChild(data.child))
        assertNotSame(data.sameChild, data.root.getChild(data.sameChild))
        assertEquals(data.child, data.root.add(data.sameChild))
    }

    @Test
    fun testAddTo() {
        val data = Data()
        var ret = data.child.addTo(data.root)

        assertSame(ret, data.root)

        assertEquals(data.child, data.root.getChild(data.child))
        assertTrue(data.child.isLeaf)
        assertFalse(data.root.isLeaf)
        assertSame(data.root, data.child.root)

        ret = data.sameChild.addTo(data.root)

        assertSame(data.root, ret)
        assertSame(data.child, data.root.getChild(data.sameChild))

        data.empty.addToCollapseEmpty(data.child)
        assertTrue(data.child.isLeaf)

        ret = data.leaf.addTo(data.empty).addToSkipEmpty(data.child)
        assertSame(data.child, ret)
        assertTrue(data.child.isLeaf)

        ret = data.leaf.addTo(data.empty).addToCollapseEmpty(data.child)
        assertSame(data.child, ret)
        assertFalse(data.child.isLeaf)
    }

    @Test
    fun testRemove() {
        val data = Data()
        data.root.add(data.child)
        assertTrue(data.root.contains(data.child))
        assertSame(data.root, data.child.parent)
        data.root.remove(data.child)
        assertFalse(data.root.contains(data.child))
        assertTrue(data.root.isLeaf)
        assertNull(data.child.parent)

        data.root.add(data.child).add(data.sameChild)

        data.sameChild.removeBranch()
        assertTrue(data.root.isLeaf)

        data.root.add(data.child).addSibling(data.child2)
        assertEquals(2, data.root.childCount)
        val list = ArrayList<Node<String, NodeType>>()
        list.add(data.child)
        list.add(data.child2)
        data.root.removeAll(list)
        assertTrue(data.root.isLeaf)

        data.root.destroy()
        data.root.add(data.child).addSibling(data.child2).add(data.leaf)
        data.leaf.removeBranch()
        assertTrue(data.child.isLeaf)

        data.root.destroy()
        data.root.add(data.child).addSibling(data.child2).add(data.leaf)
        data.leaf.removeBranch()
        assertTrue(data.child.isLeaf)
    }

    @Test
    fun testReplaceAll() {
        val data = Data()
        data.root.replace(data.child).replace(data.leaf)
        data.root2.replace(data.child2).replace(data.leaf2)

        data.root.replaceAll(data.root2.getChildren())

        assertEquals(2, data.root.childCount)
        assertTrue(data.root.contains(data.child))
        assertTrue(data.root.contains(data.child2))

        data.root.destroy()
        data.root2.destroy()
        data.root.replace(data.child).replace(data.leaf)
        data.root2.replace(data.sameChild).replace(data.child2).replace(data.leaf2)

        data.root.replaceAll(data.root2.getChildren())

        assertEquals(1, data.root.childCount)
        assertEquals(false, data.root.getChild(data.child)?.contains(data.leaf))
        assertSame(data.sameChild, data.root.getChild(data.child))
        assertSame(1, data.root.getChild(data.child)?.childCount)
        assertSame(1, data.root.leaves.size)

        data.root.destroy()
        data.root2.destroy()
        data.root.replace(data.child).replace(data.leaf)
        data.root2.replace(data.sameChild).replace(data.empty).replace(data.leaf2)
    }

    @Test
    fun testReplaceCollapseEmpty() {
        val data = Data()
        data.root.replace(data.child).replace(data.leaf)
        data.sameChild.replace(data.empty).replace(data.leaf2)

        val ret = data.root.replaceCollapseEmpty(data.sameChild)

        assertSame(data.sameChild, ret)
        assertEquals(1, data.root.childCount)
        assertEquals(false, data.root.getChild(data.child)?.contains(data.leaf))
        assertEquals(true, data.root.getChild(data.child)?.contains(data.leaf2))
    }

    @Test
    fun testReplaceSkipEmpty() {
        val data = Data()
        val ret = data.root.replace(data.child).replaceSkipEmpty(data.empty).replaceSkipEmpty(data.leaf2)

        assertSame(data.leaf2, ret)
        assertEquals(1, data.root.childCount)
        assertEquals(true, data.root.getChild(data.child)?.isLeaf)
    }

    @Test
    fun testAddCollapseEmpty() {
        val data = Data()
        data.root.replace(data.child).replace(data.leaf)
        data.sameChild.replace(data.empty).replace(data.leaf2)

        val ret = data.root.addCollapseEmpty(data.sameChild)

        assertSame(data.child, ret)
        assertEquals(1, data.root.childCount)
        assertEquals(true, data.root.getChild(data.child)?.contains(data.leaf))
        assertEquals(false, data.root.getChild(data.child)?.contains(data.empty))
        assertEquals(true, data.root.getChild(data.child)?.contains(data.leaf2))
    }

    @Test
    fun testAddSkipEmpty() {
        val data = Data()
        data.root.replace(data.child).replace(data.leaf)
        data.sameChild.replace(data.empty).replace(data.leaf2)

        val ret = data.root.addSkipEmpty(data.sameChild)

        assertSame(data.child, ret)
        assertEquals(1, data.root.childCount)
        assertEquals(false, data.root.getChild(data.child)?.isLeaf)
        assertEquals(true, data.root.getChild(data.child)?.contains(data.leaf))
        assertEquals(false, data.root.getChild(data.child)?.contains(data.empty))
        assertEquals(false, data.root.getChild(data.child)?.contains(data.leaf2))
    }

    @Test
    fun testAddAll() {
        val data = Data()
        data.root.replace(data.child).replace(data.leaf)
        data.root2.replace(data.child2).replace(data.leaf2)

        val ret = data.root.addAll(data.root2.getChildren())

        assertSame(data.root, ret)
        assertEquals(2, data.root.childCount)
        assertTrue(data.root.contains(data.child))
        assertTrue(data.root.contains(data.child2))

        data.root.destroy()
        data.root2.destroy()

        data.root.replace(data.child).replace(data.leaf)
        data.root2.replace(data.sameChild).replace(data.child2).replace(data.leaf2)

        data.root.addAll(data.root2.getChildren())

        assertEquals(1, data.root.childCount)
        assertEquals(true, data.root.getChild(data.child)?.contains(data.leaf))
        assertSame(data.child, data.root.getChild(data.sameChild))
        assertSame(2, data.root.getChild(data.child)?.childCount)
        assertSame(2, data.root.leaves.size)
    }

    @Test
    fun testJoinChildTree() {
        val data = Data()
        data.root.replace(data.child).replace(data.child2)
        data.child.replace(data.leaf)
        data.root2.replace(data.sameChild2).replace(data.sameChild)

        data.root.joinChildTree(data.root2)

        assertFalse(data.sameChild2.isLeaf)
        assertTrue(data.child.contains(data.leaf))
        assertEquals(1, data.sameChild2.childCount)
        assertSame(data.sameChild2, data.child.getChild(data.child2))
    }

    @Test
    fun testIntersectChildTree() {
        val data = Data()
        data.root.replace(data.child).replace(data.child2)
        data.child.replace(data.leaf)
        data.root2.replace(data.sameChild2).replace(data.sameChild)

        data.root.intersectChildTree(data.root2)

        assertFalse(data.sameChild2.isLeaf)
        assertFalse(data.child.contains(data.leaf))
        assertEquals(1, data.sameChild2.childCount)
        assertSame(data.sameChild2, data.child.getChild(data.child2))
    }

    @Test
    fun testMerge() {
        val data = Data()
        data.root.add(data.child).add(data.child2)
        data.child.add(data.leaf)

        data.root2.add(data.sameChild).addSibling(data.sameChild2).add(data.sameChild)

        data.root.merge(data.root2)

        assertEquals(5, data.root.size())
        assertTrue(data.root.contains(data.sameChild2))
        assertEquals(true, data.root.getChild(data.sameChild2)?.contains(data.sameChild))
    }

    @Test
    fun testSubTree() {
        val data = Data()
        data.root.replace(data.child)
        data.root.replace(data.child2)
        data.root.replace(data.leaf)
        data.root.replace(data.leaf2)
        assertEquals(4, data.root.childCount)

        var sub = data.root.subTree(1, 1)
        assertEquals(1, sub.childCount)

        sub = data.root.subTree(0, 0)
        assertEquals(0, sub.childCount)

        sub = data.root.subTree(1, 0)
        assertEquals(0, sub.childCount)

        sub = data.root.subTree(1, 2)
        assertEquals(2, sub.childCount)

        sub = data.root.subTree(3, 1)
        assertEquals(1, sub.childCount)

        sub = data.root.subTree(data.child)
        assertEquals(4, sub.childCount)

        sub = data.root.subTree(data.child, data.leaf2)
        assertEquals(4, sub.childCount)

        sub = data.root.subTree(data.leaf2)
        assertEquals(1, sub.childCount)
    }

    @Test
    fun testFind() {
        val data = Data()
        val n1 = Node("1_N", NodeType.N1)
        val l1 = Node("1_L", NodeType.LEAF)
        data.root.replace(data.child).replace(data.child2).replace(data.leaf)
        data.root.replace(n1).replace(l1).replace(Node(data.leaf.key, NodeType.N2))
        var found = data.root.find(data.leaf.key)
        assertEquals(6, found.size())
        assertEquals(2, found.leaves.size)

        data.root.destroy()
        data.root.replace(data.child).replace(data.child2).replace(data.leaf)
        data.root.replace(n1).replace(l1).replace(Node(data.leaf.key, NodeType.N2))
        found = data.root.find(data.leaf.key, filterN2)
        assertEquals(3, found.size())
        found = data.root.find(n1.key, filterN1)
        assertEquals(3, found.size())
        found = data.root.find(n1.key, filterN2)
        assertEquals(0, found.size())

        data.root.destroy()
        data.root.replace(data.child).replace(data.child2).replace(data.leaf).replace(Node("matchA"))
            .replaceSibling(Node("Nomatch"))
        data.root.replace(n1).replace(l1).replace(data.leaf.cloneNode()).replace(Node("matchB"))

        found = data.root.filteredTree({ node -> node.key?.startsWith("match") ?: false })
        assertEquals(2, found.leaves.size)
    }

    @Test
    fun testBranch() {
        val data = Data()
        val n1 = Node<String, NodeType>("1_N")
        val l1 = Node<String, NodeType>("1_L")
        data.root.replace(data.child).replace(data.child2).replace(data.leaf)
        data.child.replace(n1).replace(l1)
        var branch = data.leaf.branch
        assertEquals(2, branch.size())
        assertEquals(data.child.key, branch.key)
        branch = data.leaf.getBranch(data.child)
        assertEquals(1, branch.size())
        assertEquals(data.child2.key, branch.key)
    }

    @Test
    fun testCloneTree() {
        val data = Data()
        data.root.replace(data.child).replace(data.child2).replace(data.leaf).replaceSibling(data.leaf2)
        val clonedRoot = data.root.cloneTree()
        assertNotSame(data.root, clonedRoot)
        assertEquals(data.root.size(), clonedRoot.size())
        for (node in data.root.getChildren()) {
            assertNotSame(node, clonedRoot.getChild(node))
        }
    }

    @Test
    fun testCloneTreeSubClasses() {
        val data = Data()
        val sub1 = Node("sub1", NodeType.N1)
        data.root.replace(sub1)
        val clonedRoot = data.root.cloneTree()
        assertEquals(NodeType.N1, clonedRoot.getChild(sub1)?.value)
        assertNotSame(sub1, clonedRoot.getChild(sub1))
    }

    @Test
    fun testTraverse() {
        val data = Data()
        data.root.replace(data.child).replace(data.leaf)
        data.root.replace(data.child2).replace(data.leaf).replaceSibling(data.leaf2)

        val result = ArrayList<String?>()

        data.root.traverseByDepthTopDown({ node -> result.add(node.key) })
        assertEquals(6, result.size)
        assertNull(result[0])
        assertEquals(data.child.key, result[1])
        assertEquals(data.leaf.key, result[2])
        assertEquals(data.child2.key, result[3])
        assertEquals(data.leaf.key, result[4])
        assertEquals(data.leaf2.key, result[5])

        result.clear()
        data.root.traverseByDepthBottomUp({ node -> result.add(node.key) })
        assertEquals(6, result.size)
        assertEquals(data.leaf.key, result[0])
        assertEquals(data.child.key, result[1])
        assertEquals(data.leaf.key, result[2])
        assertEquals(data.leaf2.key, result[3])
        assertEquals(data.child2.key, result[4])
        assertNull(result[5])

        result.clear()
        data.root.traverseByBreadthTopDown({ node -> result.add(node.key) })
        assertEquals(6, result.size)
        assertNull(result[0])
        assertEquals(data.child.key, result[1])
        assertEquals(data.child2.key, result[2])
        assertEquals(data.leaf.key, result[3])
        assertEquals(data.leaf.key, result[4])
        assertEquals(data.leaf2.key, result[5])

        result.clear()
        data.root.traverseByBreadthBottomUp({ node -> result.add(node.key) })
        assertEquals(6, result.size)
        assertEquals(data.leaf.key, result[0])
        assertEquals(data.leaf.key, result[1])
        assertEquals(data.leaf2.key, result[2])
        assertEquals(data.child.key, result[3])
        assertEquals(data.child2.key, result[4])
        assertNull(result[5])
    }

    @Test
    fun testReplaceSibling() {
        val data = Data()
        var ret = data.root.replace(data.child).replaceSibling(data.leaf)
        assertSame(data.leaf, ret)
        assertEquals(2, data.root.childCount)
        assertTrue(data.root.contains(data.child))
        assertTrue(data.root.contains(data.leaf))
        ret = data.child2.replaceSiblingTo(data.child)
        assertSame(data.child, ret)
        assertEquals(3, data.root.childCount)
        assertTrue(data.root.contains(data.child2))
    }

    @Test
    fun testAddSibling() {
        val data = Data()
        var ret = data.root.add(data.child).addSibling(data.sameChild).addSibling(data.child2).addSibling(data.leaf)
        assertSame(data.leaf, ret)
        assertEquals(3, data.root.childCount)
        assertTrue(data.root.contains(data.child))
        assertTrue(data.root.contains(data.leaf))
        assertTrue(data.root.contains(data.child2))
        ret = data.sameChild2.addSiblingTo(data.child)
        assertSame(data.child, ret)
        assertEquals(3, data.root.childCount)
        assertTrue(data.root.contains(data.child2))
    }

    @Test
    fun testSubNode() {
        val data = Data()
        val node1 = Node("Node1", NodeType.N1)
        val node2 = Node("Node2", NodeType.N2)
        val node2Empty = Node<String, NodeType>(null, NodeType.N2)
        val node3 = Node("Node3", NodeType.N3)

        assertEquals(NodeType.N1, node1.value)

        data.root.add(node1).add(node2).add(node3)
        assertTrue(data.root.contains(node1))
        assertTrue(node1.contains(node2))
        assertTrue(node2.contains(node3))
        assertTrue(node3.isLeaf)
        assertSame(node1, node3.getAncestor(filterN1))
        assertSame(node1, node1.getAncestor(filterN1))

        data.root.destroy()
        data.root.add(node3.replaceTo(node2).replaceTo(node1))
        assertTrue(data.root.contains(node1))
        assertTrue(node1.contains(node2))
        assertTrue(node2.contains(node3))
        assertTrue(node3.isLeaf)

        data.root.destroy()
        data.root.add(node1).add(node2Empty).add(node3)
        assertTrue(data.root.contains(node1))
        assertTrue(node1.contains(node2Empty))
        assertTrue(node2Empty.contains(node3))
        assertTrue(node3.isLeaf)

        data.root.destroy()
        data.root.add(node1).addCollapseEmpty(node2Empty).add(node3)
        assertTrue(data.root.contains(node1))
        assertFalse(node1.contains(node2Empty))
        assertFalse(node2Empty.contains(node3))
        assertTrue(node1.contains(node3))
        assertTrue(node3.isLeaf)

        data.root.destroy()
        data.root.addSkipEmpty(node1)?.addSkipEmpty(node2Empty)?.addSkipEmpty(node3)
        assertTrue(data.root.contains(node1))
        assertFalse(node1.contains(node2Empty))
        assertFalse(node2Empty.contains(node3))
        assertFalse(node1.contains(node3))
        assertTrue(node1.isLeaf)
    }

    @Test
    fun testComparator() {
        val root = NodeComparator(null)
        root.replace(NodeComparator("A"))
        root.replace(NodeComparator("B"))
        root.replace(NodeComparator("C"))
        root.replace(NodeComparator("Z"))
        assertEquals("Z", root.firstChild?.key)
    }

    internal class NodeComparator(data: String?) : Node<String, NodeType>(data, null, TestComparator()) {

        private class TestComparator : Comparator<String?> {
            override fun compare(s1: String?, s2: String?): Int {
                return NullSafeComparator().compare(s2, s1)
            }
        }
    }

}
