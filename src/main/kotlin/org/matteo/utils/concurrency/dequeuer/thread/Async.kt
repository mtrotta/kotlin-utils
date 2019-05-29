package org.matteo.utils.concurrency.dequeuer.thread

import java.util.concurrent.Executors
import java.util.concurrent.Future

object Async {

    fun run(runnable: Runnable): Future<*> {
        val service = Executors.newSingleThreadExecutor()
        try {
            return service.submit(runnable)
        } finally {
            service.shutdown()
        }
    }

}
