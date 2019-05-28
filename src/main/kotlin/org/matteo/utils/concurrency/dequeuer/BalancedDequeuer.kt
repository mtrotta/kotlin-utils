package org.matteo.utils.concurrency.dequeuer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import org.matteo.utils.concurrency.exception.ExceptionHandler
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 23/05/19
 */
class BalancedDequeuer<T> : BasicDequeuer<T> {

    private val workers: List<BalancedWorker<T>>

    private var minWorkers: Int = 0
    private var maxWorkers: Int = 0
    private var numWorkers: AtomicInteger = AtomicInteger()
    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val reference = TreeMap<Int, Long>()

    var profile = Profile.MEDIUM

    constructor(
        processor: Processor<T>,
        capacity: Int = Channel.Factory.RENDEZVOUS,
        min: Int = DEFAULT_MIN,
        max: Int = DEFAULT_MAX,
        initial: Int = min,
        exceptionHandler: ExceptionHandler = ExceptionHandler(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        profile: Profile = Profile.MEDIUM
    ) : this(
        0.rangeTo(max).map { BalancedWorker(processor, it < initial, profile) },
        min,
        max,
        initial,
        capacity,
        exceptionHandler,
        dispatcher,
        profile
    )

    constructor(
        processors: List<Processor<T>>,
        capacity: Int = Channel.Factory.RENDEZVOUS,
        min: Int = DEFAULT_MIN,
        initial: Int = min,
        exceptionHandler: ExceptionHandler = ExceptionHandler(),
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        profile: Profile = Profile.MEDIUM
    ) : this(
        processors.mapIndexed { index, processor -> BalancedWorker(processor, index < initial, profile) },
        min,
        processors.size,
        initial,
        capacity,
        exceptionHandler,
        dispatcher,
        profile
    )

    private constructor(
        workers: List<BalancedWorker<T>>,
        min: Int,
        max: Int,
        initial: Int,
        capacity: Int,
        exceptionHandler: ExceptionHandler,
        dispatcher: CoroutineDispatcher,
        profile: Profile
    ) : super(workers, max, capacity, exceptionHandler, dispatcher) {
        this.workers = workers
        this.profile = profile
        startBalance(min, max, initial)
    }

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

    override suspend fun terminate() {
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
        val delta: Int,
        val fluid: Boolean
    ) {
        FAST(3, 0.9, 0.1, 0.05, 100, true),
        MEDIUM(5, 0.5, 0.5, 0.1, 10, true),
        SLOW(10, 0.1, 0.9, 0.2, 1, false)
    }

    private fun startBalance(min: Int, max: Int, initial: Int) {
        if (initial < min || initial > max) {
            throw IllegalArgumentException(
                String.format("Invalid initial value %d, must be %d <= initial <= %d", initial, min, max)
            )
        }
        setMin(min)
        setMax(max)
        numWorkers = AtomicInteger(initial)
        scheduledExecutorService.schedule(analyser, profile.period * CLOCK, UNIT)
    }

    @Synchronized
    private fun increaseWorkers() {
        try {
            val num = numWorkers.get()
            val increasedNumber = Math.min(maxWorkers, num + profile.delta)
            if (increasedNumber != num) {
                for (i in num until increasedNumber) {
                    val worker: BalancedWorker<T> = workers[i]
                    worker.start()
                }
                numWorkers.set(increasedNumber)
            }
        } catch (e: Exception) {
            exceptionHandler.handle(e)
        }
    }

    @Synchronized
    private fun decreaseWorkers() {
        val num = numWorkers.get()
        val decreasedNumber = Math.max(minWorkers, num - profile.delta)
        if (num != decreasedNumber) {
            for (i in decreasedNumber until num) {
                val worker: BalancedWorker<T> = workers[i]
                worker.stop()
            }
            numWorkers.set(decreasedNumber)
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

    private fun setMin(min: Int) {
        if (min < DEFAULT_MIN) {
            throw IllegalArgumentException("Invalid minimum $min, must be at least $DEFAULT_MIN")
        }
        minWorkers = min
    }

    private fun setMax(max: Int) {
        if (max < minWorkers) {
            throw IllegalArgumentException("Invalid maximum $max, must be greater than minimum $minWorkers")
        }
        maxWorkers = max
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BalancedDequeuer::class.java)

        private const val DEFAULT_MIN = 1
        private val DEFAULT_MAX = Runtime.getRuntime().availableProcessors() * 100

        private val UNIT = TimeUnit.NANOSECONDS
        val CLOCK = UNIT.convert(1, TimeUnit.SECONDS)
    }
}

internal class BalancedWorker<T>(
    processor: Processor<T>,
    working: Boolean,
    private val profile: BalancedDequeuer.Profile
) :
    Worker<T>(processor, working) {

    private val ctr = AtomicLong()

    internal val isObservable = AtomicBoolean(false)

    internal var averageWorkTime = BalancedDequeuer.CLOCK.toDouble()

    internal val processed: Long
        get() {
            isObservable.set(false)
            return ctr.getAndSet(0)
        }

    override suspend fun process(item: T?) {
        if (item != null) {
            val begin = System.nanoTime()
            super.process(item)
            val end = System.nanoTime()
            feed(1)
            val time = end - begin
            averageWorkTime = averageWorkTime * profile.low + time * profile.high
        } else {
            feed(0)
        }
    }

    private fun feed(delta: Long) {
        ctr.addAndGet(delta)
        isObservable.set(true)
    }

}
