package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
internal class BasicDequeuerTest {

    class StringProcessor(private val name: String) : Processor<String> {
        val ctr = AtomicInteger()

        override suspend fun process(item: String) {
            delay(1)
            ctr.incrementAndGet()
        }

        override fun toString(): String {
            return "StringProcessor(name='$name')"
        }
    }

    @Test
    fun test() {
        val stringProcessor = StringProcessor("1")
        val dequeuer = BasicDequeuer(stringProcessor)
        val num = 1 shl 5
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            dequeuer.awaitTermination()
        }
        assertEquals(num, stringProcessor.ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testTimeout() {
        val stringProcessor = StringProcessor("1")
        val dequeuer = BasicDequeuer(stringProcessor, capacity = Channel.UNLIMITED)
        val num = 1 shl 12
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            dequeuer.shutdown()
            try {
                dequeuer.awaitTermination(1, TimeUnit.MILLISECONDS)
            } catch (ignore: TimeoutCancellationException) {
            }
        }
        assertTrue(num != stringProcessor.ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testShutdown() {
        val stringProcessor = StringProcessor("1")
        val dequeuer = BasicDequeuer(stringProcessor)
        runBlocking {
            dequeuer.enqueue("1")
            dequeuer.shutdown()
            try {
                dequeuer.enqueue("1")
                fail()
            } catch (e: RejectedException) {
            }
            assertFalse(dequeuer.isTerminated)
            dequeuer.awaitTermination()
            assertTrue(dequeuer.isTerminated)
        }
    }

    @Test
    fun testMultiProcessor() {
        val processors = ArrayList<Processor<String>>()
        for (i in 0 until 10) {
            processors.add(StringProcessor(i.toString()))
        }
        val dequeuer = BasicDequeuer(processors)
        val num = 1 shl 10
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            println("Queue full")
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testBadProcessor() {
        val ctr = AtomicInteger()
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = BasicDequeuer(processor)
        val num = 5
        runBlocking {
            try {
                for (i in 0 until num) {
                    dequeuer.enqueue(i.toString())
                }
                fail()
            } catch (ignore: RejectedException) {
            }
            try {
                dequeuer.awaitTermination()
                fail()
            } catch (e: Exception) {
                assertSame(SIMULATED_EXCEPTION, e)
            }
        }
        assertEquals(1, ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    @Throws(Exception::class)
    fun testBadProcessorWithShutdownAction() {
        var sentinel = false
        val ctr = AtomicInteger()
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                delay(1000)
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = BasicDequeuer(processor)
        dequeuer.exceptionHandler.register { sentinel = true }
        runBlocking {
            dequeuer.enqueue("A")
            try {
                dequeuer.awaitTermination(1, TimeUnit.HOURS)
            } catch (e: Exception) {
                assertSame(SIMULATED_EXCEPTION, e)
            }
        }
        assertEquals(1, ctr.get())
        assertEquals(0, dequeuer.unprocessed.size)
        assertTrue(dequeuer.isTerminated)
        assertSame(SIMULATED_EXCEPTION, dequeuer.exceptionHandler.exception)
        assertTrue(sentinel)
    }

    @Test
    fun testBadProcessorWithShutdownActionUnprocessed() {
        var sentinel = false
        val processor = object : Processor<String> {
            val ctr = AtomicInteger()
            override suspend fun process(item: String) {
                if (ctr.incrementAndGet() > 1) {
                    throw SIMULATED_EXCEPTION
                }
                delay(1000)
            }
        }
        val dequeuer = BasicDequeuer(processor, capacity = 20)
        dequeuer.exceptionHandler.register { sentinel = true }
        runBlocking {
            try {
                dequeuer.enqueue("A")
                dequeuer.enqueue("B")
                dequeuer.enqueue("C")
            } catch (ignore: RejectedException) {
                fail()
            }

            try {
                dequeuer.awaitTermination(1, TimeUnit.HOURS)
            } catch (e: Exception) {
                assertSame(SIMULATED_EXCEPTION, e)
            }

        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(1, dequeuer.unprocessed.size)
        assertSame(SIMULATED_EXCEPTION, dequeuer.exceptionHandler.exception)
        assertTrue(sentinel)
    }

    @Test
    fun testShutdownDownNow() {
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                println("Process $item")
                delay(10000)
            }
        }
        val dequeuer = BasicDequeuer(processor, capacity = 10)
        runBlocking {
            try {
                dequeuer.enqueue("A")
                dequeuer.enqueue("B")
                dequeuer.enqueue("C")
            } catch (ignore: RejectedException) {
            }

            delay(1000)
            dequeuer.shutdownNow()
        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(2, dequeuer.unprocessed.size)
        assertNull(dequeuer.exceptionHandler.exception)
    }

    companion object {
        private val SIMULATED_EXCEPTION = RuntimeException("Simulated exception")
    }

}
