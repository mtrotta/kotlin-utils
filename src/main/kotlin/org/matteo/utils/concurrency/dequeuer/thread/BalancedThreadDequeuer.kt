package org.matteo.utils.concurrency.dequeuer.thread

import kotlinx.coroutines.runBlocking
import org.matteo.utils.concurrency.dequeuer.Processor
import org.matteo.utils.concurrency.dequeuer.coroutine.BalancedCoroutineDequeuer
import org.matteo.utils.concurrency.exception.ExceptionHandler
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/07/12
 */
class BalancedThreadDequeuer<T> : ThreadDequeuer<T> {

    private val workers: MutableList<BalancedWorker<T>> = mutableListOf()

    private var minWorkers: Int = 0
    private var maxWorkers: Int = 0
    private var numWorkers: AtomicInteger = AtomicInteger()
    private val scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(NamedThreadFactory("DequeuerBalancer"))
    private val reference = TreeMap<Int, Long>()

    private val profile: Profile

    private val analyser = {
        try {
            when (workerStatus) {
                WorkerStatus.INCREASE -> increaseWorkers()
                WorkerStatus.DECREASE, WorkerStatus.IDLE -> decreaseWorkers()
                else -> {
                }
            }
        } catch (e: Exception) {
            exceptionHandler.handle(e)
        }
    }

    private val workerStatus: WorkerStatus
        @Synchronized get() {
            var status = WorkerStatus.UNAVAILABLE
            var analysisPeriod = profile.period * CLOCK
            val results = workers.filter { it.isObservable.get() }.map { it.processed }
            if (results.isNotEmpty()) {
                val num = numWorkers.get()
                val throughput = getThroughput(results)
                if (throughput == 0L) {
                    status = WorkerStatus.IDLE
                } else {
                    status = WorkerStatus.STABLE
                    reference[num] = throughput
                    val higherKey = num + 1
                    val lowerKey = num - 1
                    val higher = reference[higherKey]
                    val lower = reference[lowerKey]
                    val direction = compare(throughput, lower, higher)
                    if (num < maxWorkers && (direction == null || direction > 0)) {
                        status = WorkerStatus.INCREASE
                    } else if (num > minWorkers && (direction == null || direction < 0)) {
                        status = WorkerStatus.DECREASE
                    } else if (profile.fluid) {
                        reference.remove(higherKey)
                        reference.remove(lowerKey)
                    }
                }
                val averageWorkTime = workers.filter { it.working }.map { it.averageWorkTime }.average()
                if (analysisPeriod < averageWorkTime * profile.period) {
                    analysisPeriod = (profile.period * averageWorkTime).toLong()
                    if (LOGGER.isDebugEnabled) {
                        LOGGER.debug("Adjusting analysis period to {}", analysisPeriod)
                    }
                }
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug(
                        String.format(
                            "Load: %s - Workers: %02d - Current: %d - Reference: %s - Results (%d): %s",
                            status,
                            num,
                            throughput,
                            reference.toString(),
                            results.size,
                            results.toString()
                        )
                    )
                }
            }
            scheduledExecutorService.schedule(analyser, analysisPeriod, UNIT)
            return status
        }

    @JvmOverloads
    constructor(
        processor: Processor<T>,
        synchronous: Boolean = true,
        min: Int = DEFAULT_MIN,
        max: Int = DEFAULT_MAX,
        initial: Int = min,
        profile: Profile = Profile.MEDIUM,
        exceptionHandler: ExceptionHandler = ExceptionHandler()
    ) : super(synchronous, exceptionHandler) {
        this.profile = profile
        for (i in 0 until max) {
            addWorker(processor)
        }
        startBalance(min, max, initial)
    }

    @JvmOverloads
    constructor(
        processors: Collection<Processor<T>>,
        synchronous: Boolean,
        min: Int,
        initial: Int = min,
        profile: Profile = Profile.MEDIUM,
        exceptionHandler: ExceptionHandler = ExceptionHandler()
    ) : super(synchronous, exceptionHandler) {
        this.profile = profile
        for (processor in processors) {
            addWorker(processor)
        }
        startBalance(min, processors.size, initial)
    }

    private fun addWorker(processor: Processor<T>) {
        val worker = BalancedWorker(processor, this, profile)
        workers.add(worker)
    }

    override fun getWorkers() = workers

    override fun terminate() {
        super.terminate()
        scheduledExecutorService.shutdownNow()
    }

    private enum class WorkerStatus {
        INCREASE,
        STABLE,
        DECREASE,
        IDLE,
        UNAVAILABLE
    }

    enum class Profile constructor(
        val period: Int,
        val high: Double,
        val low: Double,
        val worth: Double,
        val fluid: Boolean
    ) {
        FAST(3, 0.9, 0.1, 0.05, true),
        MEDIUM(5, 0.5, 0.5, 0.1, true),
        SLOW(10, 0.1, 0.9, 0.2, false)
    }

    private fun startBalance(min: Int, max: Int, initial: Int) {
        if (initial < min || initial > max) {
            throw IllegalArgumentException(
                String.format(
                    "Invalid initial value %d, must be %d <= initial <= %d",
                    initial,
                    min,
                    max
                )
            )
        }
        setMinThread(min)
        setMaxThread(max)
        numWorkers = AtomicInteger(initial)
        for (i in 0 until initial) {
            startWorker(workers[i])
        }
        scheduledExecutorService.schedule(analyser, profile.period * CLOCK, UNIT)
    }

    @Synchronized
    private fun increaseWorkers() {
        try {
            val num = numWorkers.get()
            if (num < maxWorkers) {
                val worker = workers[numWorkers.getAndIncrement()]
                startWorker(worker)
            }
        } catch (e: Exception) {
            exceptionHandler.handle(e)
        }

    }

    @Synchronized
    private fun decreaseWorkers() {
        if (numWorkers.get() > minWorkers) {
            val worker = workers[numWorkers.decrementAndGet()]
            worker.shutdown()
        }
    }

    private fun compare(current: Long, lower: Long?, higher: Long?): Int? {
        if (higher != null && lower != null) {
            return if (higher > lower) {
                if (isWorth(higher.toDouble(), current.toDouble())) 1 else 0
            } else {
                if (lower >= current) -1 else 0
            }
        } else if (higher != null) {
            return if (isWorth(higher.toDouble(), current.toDouble())) 1 else -1
        } else if (lower != null) {
            return if (lower >= current) -1 else 1
        }
        return null
    }

    private fun isWorth(val1: Double, val2: Double): Boolean {
        return (val1 - val2) / val2 > profile.worth
    }

    private fun getThroughput(data: Collection<Long>): Long {
        var total: Long = 0
        for (value in data) {
            total += value
        }
        return total
    }

    private fun setMinThread(min: Int) {
        if (min < DEFAULT_MIN) {
            throw IllegalArgumentException("Invalid minimum $min, must be at least $DEFAULT_MIN")
        }
        minWorkers = min
    }

    private fun setMaxThread(max: Int) {
        if (max < minWorkers) {
            throw IllegalArgumentException("Invalid maximum $max, must be greater than minimum $minWorkers")
        }
        maxWorkers = max
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BalancedThreadDequeuer::class.java)
        private const val DEFAULT_MIN = 1
        private val DEFAULT_MAX = Runtime.getRuntime().availableProcessors()
    }
}

internal class BalancedWorker<T>(
    processor: Processor<T>,
    dequeuer: ThreadDequeuer<T>,
    private val profile: BalancedThreadDequeuer.Profile
) : Worker<T>(processor, dequeuer) {

    private val ctr = AtomicLong()

    var averageWorkTime = BalancedCoroutineDequeuer.CLOCK.toDouble()

    internal val isObservable: AtomicBoolean = AtomicBoolean()

    internal val processed: Long
        get() {
            val processed = ctr.get()
            ctr.set(0)
            isObservable.set(false)
            return processed
        }

    override fun run() {
        try {
            dequeuer.phaser.register()
            synchronized(this) {
                var working = true
                while (working) {
                    val t: T? = dequeuer.queue.poll(ThreadDequeuer.CLOCK, ThreadDequeuer.UNIT)
                    when {
                        t != null -> {
                            val begin = System.nanoTime()
                            runBlocking {
                                processor.process(t)
                            }
                            val end = System.nanoTime()
                            feed(1)
                            val time = end - begin
                            averageWorkTime = averageWorkTime * profile.low + time * profile.high
                            dequeuer.onCompleteAction?.invoke(t)
                        }
                        shutdown -> working = false
                        else -> feed(0)
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

    private fun feed(delta: Long) {
        ctr.addAndGet(delta)
        isObservable.set(true)
    }

}

