package org.matteo.utils.concurrency.dequeuer.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.toList
import org.matteo.utils.concurrency.dequeuer.Processor
import org.matteo.utils.concurrency.dequeuer.RejectedException
import org.matteo.utils.concurrency.dequeuer.SingleDequeuer
import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.concurrent.TimeUnit

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 20/05/19
 */
open class CoroutineDequeuer<T> internal constructor(
    workers: List<Worker<T>>,
    active: Int,
    capacity: Int,
    exceptionHandler: ExceptionHandler,
    dispatcher: CoroutineDispatcher
) :
    SingleDequeuer<T>(exceptionHandler, null),
    CoroutineScope by CoroutineScope(dispatcher + CoroutineName("CoroutineDequeuer")) {

    constructor(
        processor: Processor<T>,
        workers: Int = 1,
        capacity: Int = Channel.Factory.RENDEZVOUS,
        exceptionHandler: ExceptionHandler = ExceptionHandler(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : this(
        0.rangeTo(workers).map { Worker(processor) },
        workers,
        capacity,
        exceptionHandler,
        dispatcher
    )

    constructor(
        processors: List<Processor<T>>,
        capacity: Int = Channel.Factory.RENDEZVOUS,
        exceptionHandler: ExceptionHandler = ExceptionHandler(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ) : this(
        processors.map { Worker(it) },
        processors.size,
        capacity,
        exceptionHandler,
        dispatcher
    )

    private val channel = Channel<T>(capacity)

    override var unprocessed: List<T> = listOf()

    override var isTerminated: Boolean = false

    private val job: Job

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        exceptionHandler.handle(exception)
    }

    init {
        job = launch(coroutineExceptionHandler + CoroutineName("Processor")) {
            for (i in 0 until active) {
                startWorker(channel, workers[i])
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun CoroutineScope.startWorker(channel: ReceiveChannel<T>, worker: Worker<T>) = launch {
        try {
            while (isActive) {
                if (worker.working) {
                    val item = withTimeoutOrNull(CLOCK) {
                        channel.receive()
                    }
                    worker.process(item)
                    if (item != null) {
                        onCompleteAction?.invoke(item)
                    }
                } else if (channel.isClosedForReceive) {
                    break
                } else {
                    delay(CLOCK)
                }
            }
        } catch (ignore: ClosedReceiveChannelException) {
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
        doTerminate(time, unit)
        val exception = exceptionHandler.exception
        if (exception != null) {
            throw exception
        }
    }

    override suspend fun doTerminate(time: Long, unit: TimeUnit) {
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
            terminate()
        }
    }

    protected open suspend fun terminate() {
        isTerminated = true
        unprocessed = channel.toList()
    }

    override suspend fun shutdownNow(cause: Throwable?) {
        job.cancel(CancellationException("Canceled due to exception", cause))
        awaitTermination()
    }

    companion object {
        private val CLOCK = TimeUnit.SECONDS.toMillis(1)
    }
}

internal open class Worker<in T>(private val processor: Processor<T>, var working: Boolean = true) {
    open suspend fun process(item: T?) {
        if (item != null) {
            processor.process(item)
        }
    }

    internal fun start() {
        working = true
    }

    internal fun stop() {
        working = false
    }
}

