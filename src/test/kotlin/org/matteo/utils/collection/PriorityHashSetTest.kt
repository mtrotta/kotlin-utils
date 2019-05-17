package org.matteo.utils.collection

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class PriorityHashSetTest {

    @Test
    fun testNormal() {
        val set = PriorityHashSet<Mock>()

        val id1 = "ID1"
        assertTrue(set.add(Mock(id1, 10)))
        assertTrue(set.add(Mock(id1, 9)))
        val bestId1 = Mock(id1, 8)
        assertTrue(set.add(bestId1))
        assertFalse(set.add(Mock(id1, 15)))

        val id2 = "ID2"
        assertTrue(set.add(Mock(id2, 10)))
        assertFalse(set.add(Mock(id2, 11)))
        val bestId2 = Mock(id2, -1)
        assertTrue(set.add(bestId2))

        assertEquals(2, set.size)

        for (mock in set) {
            if (id1 == mock.id) {
                assertSame(mock, bestId1)
            } else if (id2 == mock.id) {
                assertSame(mock, bestId2)
            }
        }
    }

    @Test
    fun testInverted() {
        val set = PriorityHashSet<Mock>(Mock.comparator.reversed())

        val id1 = "ID1"
        assertTrue(set.add(Mock(id1, 9)))
        assertTrue(set.add(Mock(id1, 10)))
        val bestId1 = Mock(id1, 15)
        assertFalse(set.add(Mock(id1, 8)))
        assertTrue(set.add(bestId1))

        val id2 = "ID2"
        assertTrue(set.add(Mock(id2, 11)))
        assertFalse(set.add(Mock(id2, 10)))
        val bestId2 = Mock(id2, 100)
        assertTrue(set.add(bestId2))

        assertEquals(2, set.size)

        for (mock in set) {
            if (id1 == mock.id) {
                assertSame(mock, bestId1)
            } else if (id2 == mock.id) {
                assertSame(mock, bestId2)
            }
        }
    }

    data class Mock(val id: String, val priority: Int) : Comparable<Mock> {

        companion object {
            val comparator: Comparator<Mock> = Comparator
                .comparing<Mock, String> { it.id }
                .thenComparing<Int> { it.priority }
        }

        override fun compareTo(other: Mock): Int = comparator.compare(this, other)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Mock

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }


    }

}