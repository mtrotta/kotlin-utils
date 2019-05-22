package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 22/05/19
 */
@ExperimentalCoroutinesApi
class ChainedDequeuer<T>(dequeuers: List<BasicDequeuer<T>>, val exceptionHandler: ExceptionHandler = ExceptionHandler()) : Dequeuer<T> {

    private val chain = mutableListOf<BasicDequeuer<T>>()

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
        var previous: BasicDequeuer<T>? = null
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
    override suspend fun shutdownNow(cause: Throwable?) {
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
    override suspend fun awaitTermination(time: Long, unit: TimeUnit) {
        for (dequeuer in chain) {
            dequeuer.terminate(time, unit)
        }
        val exception = exceptionHandler.exception
        if (exception != null) {
            throw exception
        }
    }

    @Throws(RejectedException::class)
    override suspend fun enqueue(item: T) {
        chain.first().enqueue(item)
    }

}
