package org.matteo.utils.concurrency.dequeuer

import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.concurrent.TimeUnit

abstract class SingleDequeuer<T>(
    var exceptionHandler: ExceptionHandler,
    var onCompleteAction: ((T) -> Unit)? = null
) : Dequeuer<T> {

    abstract fun doTerminate(time: Long, unit: TimeUnit)

}
