package org.matteo.utils.concurrency.dequeuer

import java.util.concurrent.TimeUnit

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 20/05/19
 */
interface Dequeuer<T> {

    val unprocessed: List<T>

    val isTerminated: Boolean

    @Throws(RejectedException::class)
    fun enqueue(item: T)

    fun shutdown()

    fun shutdownNow(cause: Throwable? = null)

    fun awaitTermination(time: Long = -1L, unit: TimeUnit = TimeUnit.SECONDS)

}

interface Processor<in T> {
    suspend fun process(item: T)
}

class RejectedException(message: String) : Exception(message)