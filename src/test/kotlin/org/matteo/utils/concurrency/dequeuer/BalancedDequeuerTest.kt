package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
internal class BalancedDequeuerTest {

    class StringProcessor : Processor<String> {
        val ctr = AtomicInteger()
        override suspend fun process(item: String) {
            delay(100)
            ctr.incrementAndGet()
        }
    }

    class StupidProcessor : Processor<String> {
        val ctr = AtomicInteger()
        override suspend fun process(item: String) {
            delay((if (ctr.incrementAndGet() % 4000 == 0) 10 else 0).toLong())
        }
    }

    class SmartProcessor : Processor<Collection<String>> {
        val ctr = AtomicInteger()
        override suspend fun process(item: Collection<String>) {
            for (s in item) {
                delay(0)
                ctr.incrementAndGet()
            }
            delay(10)
        }
    }

    @Test
    fun testBalanceSingleProcessor() {
        val begin = System.currentTimeMillis()
        val processor = StringProcessor()
        val dequeuer = BalancedDequeuer(processor, min = 1000, max = 5000, profile = BalancedDequeuer.Profile.FAST)
        val num = 50000
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            println("Queue full")
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(num, processor.ctr.get())
        val end = System.currentTimeMillis()
        println(end - begin)
    }

    @Test
    fun testBalance() {
        val processors = ArrayList<StringProcessor>()
        for (i in 0 until 2000) {
            processors.add(StringProcessor())
        }
        val dequeuer = BalancedDequeuer(processors, initial = 100, dispatcher = Dispatchers.IO)
        val num = 1 shl 14
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            println("Queue full")
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(num, processors.map { it.ctr.get() }.sum())
    }

    class ThreadUnsafeProcessor : Processor<String> {
        private val collection = HashSet<String>(1)
        override suspend fun process(item: String) {
            collection.add(item)
            for (s in collection) {
                delay(0)
            }
            collection.remove(item)
        }
    }

    @Test
    fun testBalanceUnsafe() {
        val processors = ArrayList<ThreadUnsafeProcessor>()
        for (i in 0..4) {
            processors.add(ThreadUnsafeProcessor())
        }
        val dequeuer = BalancedDequeuer(processors, Channel.Factory.UNLIMITED, profile = BalancedDequeuer.Profile.SLOW)
        val num = (1 shl 20).toLong()
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            delay(10000)
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testBalanceStupid() {
        val processors = ArrayList<StupidProcessor>()
        for (i in 0..200) {
            processors.add(StupidProcessor())
        }
        val dequeuer = BalancedDequeuer(processors, Channel.Factory.UNLIMITED)
        dequeuer.profile = BalancedDequeuer.Profile.FAST
        val num = 1 shl 20
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(i.toString())
            }
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(num, processors.map { it.ctr.get() }.sum())
    }

    @Test
    fun testBalanceSmart() {
        val processors = ArrayList<SmartProcessor>()
        for (i in 0..400) {
            processors.add(SmartProcessor())
        }
        val dequeuer = BalancedDequeuer(processors)
        val profile = BalancedDequeuer.Profile.FAST
        dequeuer.profile = profile
        val num = 1 shl 25
        var list: MutableList<String> = ArrayList()
        runBlocking {
            for (i in 0 until num) {
                list.add(Integer.toString(i))
                if (list.size >= 4000) {
                    dequeuer.enqueue(list)
                    list = ArrayList()
                }
            }
            dequeuer.enqueue(list)
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
        assertEquals(num, processors.map { it.ctr.get() }.sum())
    }

    @Test
    fun testBalanceBigDecimal() {
        val processor: Processor<String> = object : Processor<String> {
            override suspend fun process(item: String) {
                val bigDecimal = BigDecimal(item)
                bigDecimal.toDouble()
            }
        }
        val processors = ArrayList<Processor<String>>()
        for (i in 0..90) {
            processors.add(processor)
        }

        val dequeuer = BalancedDequeuer(processors)
        dequeuer.profile = BalancedDequeuer.Profile.SLOW
        val num = (1 shl 16).toLong()
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(Math.random().toString())
            }
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testBalanceDouble() {
        val processor: Processor<String> = object : Processor<String> {
            override suspend fun process(item: String) {
                item.toDouble()
            }
        }
        val processors = ArrayList<Processor<String>>()
        for (i in 0..90) {
            processors.add(processor)
        }
        val dequeuer = BalancedDequeuer(processors)
        val num = (1 shl 18).toLong()
        runBlocking {
            for (i in 0 until num) {
                dequeuer.enqueue(Math.random().toString())
            }
            dequeuer.awaitTermination()
        }
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    fun testQueueBadProcessor() {
        val ctr = AtomicInteger()
        val processor: Processor<String> = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = BalancedDequeuer(processor)
        val num = 15
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
            } catch (e: Exception) {
                assertSame(SIMULATED_EXCEPTION, e)
            }
        }
        assertEquals(1, ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

    companion object {
        private val SIMULATED_EXCEPTION = RuntimeException("Simulated exception")
    }

}
