package org.matteo.utils.concurrency.dequeuer.thread

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.matteo.utils.concurrency.dequeuer.Processor
import org.matteo.utils.concurrency.dequeuer.RejectedException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 02/07/12
 */
internal class ThreadDequeuerTest {

    private var sentinel: Boolean = false

    class StringProcessor(private val time: Long = 0) : Processor<String> {
        val ctr = AtomicInteger()
        override suspend fun process(item: String) {
            delay(time)
            ctr.incrementAndGet()
        }
    }

    @Test
    fun testQueue() {
        val threads = 10
        val processor = StringProcessor()
        val dequeuer = ThreadDequeuer(processor, threads, false)
        val num = 1 shl 10
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertEquals(num, processor.ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testQueueTimeout() {
        val threads = 1
        val processor = StringProcessor(50)
        val dequeuer = ThreadDequeuer(processor, threads, false)
        val num = 1 shl 10
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        dequeuer.awaitTermination(1, TimeUnit.NANOSECONDS)
        assertNotEquals(num, processor.ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testQueueMultiProcessor() {
        val threads = 10
        val processors = ArrayList<StringProcessor>()
        for (i in 0 until threads) {
            processors.add(StringProcessor())
        }
        val dequeuer = ThreadDequeuer(processors)
        val num = 1 shl 14
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        println("Queue full")
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testQueueBadProcessor() {
        val ctr = AtomicInteger()
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = ThreadDequeuer(processor)
        val num = 15
        try {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            fail()
        } catch (ignore: RejectedException) {
        }

        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: Exception) {
            assertSame(SIMULATED_EXCEPTION, e)
        }

        assertNotEquals(num, ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testQueueBadProcessorWithShutdownAction() {
        val ctr = AtomicInteger()
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                delay(1000)
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = ThreadDequeuer(processor)
        val exceptionHandler = dequeuer.exceptionHandler
        exceptionHandler.register { sentinel = true }
        exceptionHandler.register { t -> t.printStackTrace() }
        dequeuer.enqueue("A")
        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: Exception) {
            assertSame(SIMULATED_EXCEPTION, e)
        }

        assertEquals(1, ctr.get())
        assertEquals(0, dequeuer.unprocessed.size)
        assertTrue(dequeuer.isTerminated)
        assertSame(SIMULATED_EXCEPTION, exceptionHandler.exception)
        assertTrue(sentinel)
    }

    @Test
    fun testQueueBadProcessorWithShutdownActionTerminate() {
        val ctr = AtomicInteger()
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = ThreadDequeuer(processor)
        val exceptionHandler = dequeuer.exceptionHandler
        exceptionHandler.register { sentinel = true }
        exceptionHandler.register { t -> t.printStackTrace() }
        try {
            dequeuer.enqueue("A")
            dequeuer.enqueue("B")
            dequeuer.enqueue("C")
        } catch (ignore: RejectedException) {
        }

        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        assertTrue(dequeuer.isTerminated)
        assertSame(SIMULATED_EXCEPTION, exceptionHandler.exception)
        assertTrue(sentinel)
    }

}

val SIMULATED_EXCEPTION = RuntimeException("Simulated exception")
