package org.matteo.utils.concurrency.dequeuer.thread

import kotlinx.coroutines.runBlocking
import org.matteo.utils.concurrency.dequeuer.Processor
import org.matteo.utils.concurrency.dequeuer.RejectedException
import org.matteo.utils.concurrency.dequeuer.SingleDequeuer
import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.concurrent.*

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/07/12
 */
open class ThreadDequeuer<T> protected constructor(
    synchronous: Boolean,
    exceptionHandler: ExceptionHandler
) :
    SingleDequeuer<T>(exceptionHandler) {

    private val workers: MutableList<Worker<T>> = mutableListOf()

    internal val queue: BlockingQueue<T>

    private val service: ExecutorService = Executors.newCachedThreadPool(NamedThreadFactory("ThreadDequeuer"))
    internal val phaser = Phaser()

    override var isTerminated: Boolean = false
    private var isShutdown: Boolean = false

    override var unprocessed: List<T> = listOf()

    constructor(
        processor: Processor<T>,
        threads: Int = Runtime.getRuntime().availableProcessors(),
        synchronous: Boolean = true,
        exceptionHandler: ExceptionHandler = ExceptionHandler()
    ) : this(synchronous, exceptionHandler) {
        for (i in 0 until threads) {
            val worker = createWorker(processor)
            workers.add(worker)
            startWorker(worker)
        }
    }

    constructor(processors: Collection<Processor<T>>, synchronous: Boolean = true, exceptionHandler: ExceptionHandler = ExceptionHandler()
    ) : this(synchronous, exceptionHandler) {
        for (processor in processors) {
            val worker = createWorker(processor)
            workers.add(worker)
            startWorker(worker)
        }
    }

    init {
        queue = getQueue(synchronous)
        exceptionHandler.register { this.shutdownNow() }
        phaser.register()
    }

    private fun getQueue(synchronous: Boolean): BlockingQueue<T> {
        return if (synchronous) SynchronousQueue() else LinkedBlockingQueue()
    }

    private fun createWorker(processor: Processor<T>): Worker<T> {
        return Worker(processor, this)
    }

    internal fun startWorker(worker: Worker<T>) {
        service.submit(worker)
    }

    override fun shutdown() {
        isShutdown = true
        for (worker in getWorkers()) {
            worker.shutdown()
        }
    }

    internal open fun getWorkers(): List<Worker<T>> = workers

    @Synchronized
    override fun shutdownNow(cause: Throwable?) {
        try {
            unprocessed = run {
                val list = mutableListOf<T>()
                queue.drainTo(list)
                list
            }
            service.shutdownNow()
        } finally {
            terminate()
        }
    }

    override fun awaitTermination(time: Long, unit: TimeUnit) {
        if (!isTerminated) {
            doTerminate(time, unit)
            val exception = exceptionHandler.exception
            if (exception != null) {
                throw exception
            }
        }
    }

    override fun doTerminate(time: Long, unit: TimeUnit) {
        try {
            shutdown()
            Async.run({
                phaser.arriveAndAwaitAdvance()
                service.shutdown()
            })
            service.awaitTermination(time, unit)
        } finally {
            terminate()
        }
    }

    override fun enqueue(item: T) {
        do {
            if (isShutdown || exceptionHandler.exception != null) {
                throw RejectedException("Queue has been shutdown or an exception occurred")
            }
        } while (!queue.offer(item, CLOCK, UNIT))
    }

    protected open fun terminate() {
        isTerminated = true
    }

    internal fun setExceptionHandler(exceptionHandler: ExceptionHandler) {
        this.exceptionHandler = exceptionHandler
        exceptionHandler.register { this.shutdownNow() }
    }

    companion object {
        internal val UNIT = TimeUnit.NANOSECONDS
        internal val CLOCK = UNIT.convert(1, TimeUnit.SECONDS)
    }
}

internal open class Worker<T> internal constructor(val processor: Processor<T>, val dequeuer: ThreadDequeuer<T>) : Runnable {

    var working = true
    var shutdown = false

    override fun run() {
        try {
            dequeuer.phaser.register()
            working = true
            synchronized(this) {
                while (working) {
                    val t: T? = dequeuer.queue.poll(ThreadDequeuer.CLOCK, ThreadDequeuer.UNIT)
                    if (t != null) {
                        runBlocking {
                            processor.process(t)
                        }
                        dequeuer.onCompleteAction?.invoke(t)
                    } else if (shutdown) {
                        working = false
                    }
                }
            }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (unhandled: Exception) {
            dequeuer.exceptionHandler.handle(unhandled)
        } finally {
            dequeuer.phaser.arrive()
        }
    }

    fun shutdown() {
        shutdown = true
    }

}