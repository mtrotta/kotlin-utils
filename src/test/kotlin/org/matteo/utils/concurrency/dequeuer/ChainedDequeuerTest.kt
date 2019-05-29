package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.matteo.utils.concurrency.dequeuer.coroutine.CoroutineDequeuer
import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
internal class ChainedDequeuerTest {

    @Test
    fun testEmpty() {
        assertThrows(IllegalArgumentException::class.java, { ChainedDequeuer(listOf(), ExceptionHandler()) })
    }

    @Test
    fun testSingle() {
        val processor = FakeProcessor(0)
        val exceptionHandler = ExceptionHandler()
        val dequeuer = ChainedDequeuer(listOf(CoroutineDequeuer(processor)), exceptionHandler)
        runBlocking {
            dequeuer.enqueue("1")
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(1, processor.ctr.get())
    }

    @Test
    fun testQueue() {
        val threads = 3

        val processor1 = FakeProcessor(1)
        val processor2 = FakeProcessor(2)

        val exceptionHandler = ExceptionHandler()

        val dequeuer1 = CoroutineDequeuer(processor1, threads)
        val dequeuer2 = CoroutineDequeuer(processor2, threads)

        val chainedDequeuer = ChainedDequeuer(listOf(dequeuer1, dequeuer2), exceptionHandler)

        val num = 10

        runBlocking {
            for (i in 0 until num) {
                chainedDequeuer.enqueue(i.toString())
            }
            println("Queue full")
            chainedDequeuer.awaitTermination()
        }

        assertTrue(chainedDequeuer.isTerminated)

        assertEquals(num, processor1.ctr.get())
        assertEquals(num, processor2.ctr.get())
    }

    private inner class ConditionalBadProcessor constructor(internal val goBad: Boolean) : Processor<String> {
        internal val ctr = AtomicInteger()

        override suspend fun process(item: String) {
            ctr.incrementAndGet()
            if (goBad) {
                throw SIMULATED_EXCEPTION
            }
        }
    }

    @Test
    fun testChainedQueueBadProcessor() {
        var sentinel = false
        val processorSuccess = ConditionalBadProcessor(false)
        val dequeuer1 = CoroutineDequeuer(processorSuccess)

        val processorFail = ConditionalBadProcessor(true)
        val dequeuer2 = CoroutineDequeuer(processorFail)

        val chainedDequeuer = ChainedDequeuer(listOf(dequeuer1, dequeuer2))

        chainedDequeuer.exceptionHandler.register { sentinel = true }

        val num = 15
        runBlocking {
            try {
                for (i in 0 until num) {
                    chainedDequeuer.enqueue(i.toString())
                }
            } catch (ignore: RejectedException) {
            }

            try {
                chainedDequeuer.awaitTermination()
            } catch (e: Exception) {
                assertSame(SIMULATED_EXCEPTION, e)
            }
        }

        assertTrue(processorSuccess.ctr.get() > 0)
        assertEquals(1, processorFail.ctr.get())
        assertTrue(chainedDequeuer.isTerminated)
        assertTrue(dequeuer1.isTerminated)
        assertTrue(dequeuer2.isTerminated)
        assertSame(SIMULATED_EXCEPTION, chainedDequeuer.exceptionHandler.exception)
        assertTrue(sentinel)
    }

    private inner class FakeProcessor(val name: Int) : Processor<String> {
        internal var ctr = AtomicInteger()

        override suspend fun process(item: String) {
            delay(1)
            ctr.incrementAndGet()
        }

        override fun toString(): String {
            return "FakeProcessor(name=$name)"
        }
    }

    companion object {
        private val SIMULATED_EXCEPTION = RuntimeException("Simulated exception")
    }

}
