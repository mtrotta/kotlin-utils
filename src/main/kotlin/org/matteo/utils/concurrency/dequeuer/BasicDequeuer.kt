package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.toList
import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.concurrent.TimeUnit

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 20/05/19
 */
open class BasicDequeuer<T> private constructor(
    processors: List<Processor<T>>,
    workers: Int,
    capacity: Int,
    var exceptionHandler: ExceptionHandler
) :
    Dequeuer<T>,
    CoroutineScope by CoroutineScope(Dispatchers.Default + CoroutineName("Dequeuer")) {

    constructor(
        processor: Processor<T>,
        workers: Int = 1,
        capacity: Int = Channel.Factory.RENDEZVOUS,
        exceptionHandler: ExceptionHandler = ExceptionHandler()
    ) : this(
        listOf(
            processor
        ), workers, capacity,
        exceptionHandler
    )

    constructor(
        processors: List<Processor<T>>,
        capacity: Int = Channel.Factory.RENDEZVOUS,
        exceptionHandler: ExceptionHandler = ExceptionHandler()
    ) : this(
        processors,
        processors.size,
        capacity,
        exceptionHandler
    )

    private val channel = Channel<T>(capacity)

    override var unprocessed: List<T> = listOf()

    override var isTerminated: Boolean = false

    private val job: Job

    internal var onCompleteAction: (suspend (T) -> Unit)? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        exceptionHandler.handle(exception)
    }

    init {
        job = launch(coroutineExceptionHandler + CoroutineName("Processor")) {
            if (processors.size > 1) {
                for (processor in processors) {
                    startWorker(processor, channel)
                }
            } else {
                val processor = processors.first()
                for (i in 0 until workers) {
                    startWorker(processor, channel)
                }
            }
        }
    }

    private fun CoroutineScope.startWorker(processor: Processor<T>, channel: ReceiveChannel<T>) = launch {
        for (item in channel) {
            if (isActive) {
                processor.process(item)
                onCompleteAction?.invoke(item)
            }
        }
    }

    @ExperimentalCoroutinesApi
    override suspend fun enqueue(item: T) {
        if (isTerminated or !isActive or !job.isActive or channel.isClosedForReceive) {
            throw RejectedException("Dequeuer has been terminated")
        }
        try {
            withContext(job) {
                channel.send(item)
            }
        } catch (e: CancellationException) {
            throw RejectedException("Dequeuer has been canceled")
        }
    }

    override fun shutdown() {
        channel.close()
    }

    @Throws(Exception::class)
    override suspend fun awaitTermination(time: Long, unit: TimeUnit) {
        shutdown()
        terminate(time, unit)
        val exception = exceptionHandler.exception
        if (exception != null) {
            throw exception
        }
    }

    internal suspend fun terminate(time: Long, unit: TimeUnit) {
        shutdown()
        try {
            if (time > 0) {
                withTimeout(unit.toMillis(time)) {
                    job.join()
                }
            } else {
                job.join()
            }
        } finally {
            isTerminated = true
            unprocessed = channel.toList()
        }
    }

    override suspend fun shutdownNow(cause: Throwable?) {
        job.cancel(CancellationException("Canceled due to exception", cause))
        awaitTermination()
    }

}
