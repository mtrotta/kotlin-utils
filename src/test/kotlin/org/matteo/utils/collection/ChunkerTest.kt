package org.matteo.utils.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class ChunkerTest {

    @Test
    internal fun testEmpty() {
        chunker<Any>(listOf(), { it }) {
            fail()
        }
    }

    @Test
    internal fun testOne() {
        val chunks = mutableListOf<Collection<String>>()
        chunker(listOf("1"), { it }, chunks::add)
        assertEquals(1, chunks.size)
        assertEquals(1, chunks[0].size)
    }

    @Test
    internal fun testMany() {
        val chunks = mutableListOf<Collection<String>>()
        chunker(listOf("1", "2", "2", "3", "3", "3"), { it }, chunks::add)
        assertEquals(3, chunks.size)
        assertEquals(1, chunks[0].size)
        assertEquals(2, chunks[1].size)
        assertEquals(3, chunks[2].size)
    }

    @Test
    internal fun testLast() {
        val chunks = mutableListOf<Collection<String>>()
        chunker(listOf("1", "2", "2", "3"), { it }, chunks::add)
        assertEquals(3, chunks.size)
        assertEquals(1, chunks[0].size)
        assertEquals(2, chunks[1].size)
        assertEquals(1, chunks[2].size)
    }

}