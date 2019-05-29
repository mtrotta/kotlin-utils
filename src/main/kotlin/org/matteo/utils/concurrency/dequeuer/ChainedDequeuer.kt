package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 22/05/19
 */
@ExperimentalCoroutinesApi
class ChainedDequeuer<T>(
    dequeuers: List<SingleDequeuer<T>>,
    val exceptionHandler: ExceptionHandler = ExceptionHandler()
) : Dequeuer<T> {

    private val chain = mutableListOf<SingleDequeuer<T>>()

    override val unprocessed: List<T>
        get() {
            val unprocessed = ArrayList<T>()
            chain.forEach { c -> unprocessed.addAll(c.unprocessed) }
            return unprocessed
        }

    override val isTerminated: Boolean
        get() {
            var terminated = true
            for (dequeuer in chain) {
                terminated = terminated and dequeuer.isTerminated
            }
            return terminated
        }

    init {
        if (dequeuers.isEmpty()) {
            throw IllegalArgumentException("An empty chained dequeuer doesn't make sense")
        }
        val iterator = dequeuers.iterator()
        var previous: SingleDequeuer<T>? = null
        while (iterator.hasNext()) {
            val next = iterator.next()
            chain.add(next)
            next.exceptionHandler = exceptionHandler
            if (previous != null) {
                previous.onCompleteAction = next::enqueue
            }
            previous = next
        }
    }

    @Synchronized
    override fun shutdownNow(cause: Throwable?) {
        for (dequeuer in chain) {
            dequeuer.shutdownNow()
        }
    }

    override fun shutdown() {
        for (dequeuer in chain) {
            dequeuer.shutdown()
        }
    }

    @Throws(Exception::class)
    override fun awaitTermination(time: Long, unit: TimeUnit) {
        runBlocking {
            for (dequeuer in chain) {
                dequeuer.doTerminate(time, unit)
            }
        }
        val exception = exceptionHandler.exception
        if (exception != null) {
            throw exception
        }
    }

    @Throws(RejectedException::class)
    override fun enqueue(item: T) {
        chain.first().enqueue(item)
    }

}
