package org.matteo.utils.concurrency.dequeuer

import org.matteo.utils.concurrency.exception.ExceptionHandler
import java.util.concurrent.TimeUnit

abstract class SingleDequeuer<T>(
    var exceptionHandler: ExceptionHandler,
    var onCompleteAction: (suspend (T) -> Unit)?
) : Dequeuer<T> {

    abstract suspend fun doTerminate(time: Long, unit: TimeUnit)

}
