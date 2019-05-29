package org.matteo.utils.concurrency.dequeuer.thread

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.matteo.utils.concurrency.dequeuer.Processor
import org.matteo.utils.concurrency.dequeuer.RejectedException
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 02/07/12
 */
internal class BalancedThreadDequeuerTest {

    class StringProcessor : Processor<String> {
        private val ctr = AtomicInteger()
        override suspend fun process(item: String) {
            delay(1)
            ctr.incrementAndGet()
        }
    }

    class StupidProcessor : Processor<String> {

        private val ctr = AtomicInteger()

        override suspend fun process(item: String) {
            delay((if (ctr.incrementAndGet() % 4000 == 0) 10 else 0).toLong())
        }

    }

    class SmartProcessor : Processor<Collection<String>> {
        override suspend fun process(item: Collection<String>) {
            for (s in item) {
                delay(0)
            }
            delay(10)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testBalanceSingleProcessor() {
        val begin = System.currentTimeMillis()
        val dequeuer = BalancedThreadDequeuer(StringProcessor(), true, 1, 20, 1)
        val num = 10000
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        println("Queue full")
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
        val end = System.currentTimeMillis()
        println(end - begin)
    }

    @Test
    @Throws(Exception::class)
    fun testBalance() {
        val processors = ArrayList<StringProcessor>()
        for (i in 0..19) {
            processors.add(StringProcessor())
        }
        val dequeuer = BalancedThreadDequeuer(processors, false, 1, 20)
        val num = 1 shl 15
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        println("Queue full")
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
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
    @Throws(Exception::class)
    fun testBalanceMin() {
        val processors = ArrayList<ThreadUnsafeProcessor>()
        for (i in 0..4) {
            processors.add(ThreadUnsafeProcessor())
        }
        val dequeuer = BalancedThreadDequeuer(processors, false, 1, 1)
        val num = (1 shl 20).toLong()
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        println("-----Before sleep-----")
        Thread.sleep(10000)
        println("-----After sleep-----")
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    @Throws(Exception::class)
    fun testBalanceStupid() {
        val processors = ArrayList<StupidProcessor>()
        for (i in 0..19) {
            processors.add(StupidProcessor())
        }
        val dequeuer = BalancedThreadDequeuer(processors, false, 1, 1, profile = BalancedThreadDequeuer.Profile.FAST)
        val num = 1 shl 18
        for (i in 0 until num) {
            dequeuer.enqueue(i.toString())
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    @Throws(Exception::class)
    fun testBalanceSmart() {
        val processors = ArrayList<SmartProcessor>()
        for (i in 0..19) {
            processors.add(SmartProcessor())
        }
        val dequeuer = BalancedThreadDequeuer(processors, true, 1, 1, profile = BalancedThreadDequeuer.Profile.FAST)
        val num = 1 shl 20
        var list: MutableList<String> = ArrayList()
        for (i in 0 until num) {
            list.add(Integer.toString(i))
            if (list.size >= 4000) {
                dequeuer.enqueue(list)
                list = ArrayList()
            }
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    @Throws(Exception::class)
    fun testBalanceBigDecimal() {
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                val bigDecimal = BigDecimal(item)
                bigDecimal.toDouble()
            }
        }
        val processors = ArrayList<Processor<String>>()
        for (i in 0..9) {
            processors.add(processor)
        }
        val dequeuer = BalancedThreadDequeuer(processors, true, 1, 1, profile = BalancedThreadDequeuer.Profile.SLOW)
        val num = (1 shl 20).toLong()
        for (i in 0 until num) {
            dequeuer.enqueue(Math.random().toString())
        }
        dequeuer.awaitTermination()
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    @Throws(Exception::class)
    fun testBalanceDouble() {
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                java.lang.Double.parseDouble(item)
            }
        }
        val processors = ArrayList<Processor<String>>()
        for (i in 0..9) {
            processors.add(processor)
        }
        val dequeuer = BalancedThreadDequeuer(processors, true, 1, 1)
        val num = (1 shl 20).toLong()
        for (i in 0 until num) {
            dequeuer.enqueue(Math.random().toString())
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS)
        assertTrue(dequeuer.isTerminated)
    }

    @Test
    @Throws(Exception::class)
    fun testQueueBadProcessor() {
        val ctr = AtomicInteger()
        val processor = object : Processor<String> {
            override suspend fun process(item: String) {
                ctr.incrementAndGet()
                throw SIMULATED_EXCEPTION
            }
        }
        val dequeuer = BalancedThreadDequeuer(processor)
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

        assertEquals(1, ctr.get())
        assertTrue(dequeuer.isTerminated)
    }

}
