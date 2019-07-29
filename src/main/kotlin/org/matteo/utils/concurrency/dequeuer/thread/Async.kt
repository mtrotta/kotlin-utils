package org.matteo.utils.concurrency.dequeuer.thread

import java.util.concurrent.Executors
import java.util.concurrent.Future

fun async(runnable: () -> Unit): Future<*> {
    val service = Executors.newSingleThreadExecutor()
    try {
        return service.submit(runnable)
    } finally {
        service.shutdown()
    }
}
