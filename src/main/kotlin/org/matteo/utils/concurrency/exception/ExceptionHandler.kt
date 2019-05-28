package org.matteo.utils.concurrency.exception

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 20/05/19
 */
class ExceptionHandler {

    private val log: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)

    var exception: Throwable? = null
        private set

    private val listeners = ArrayList<(e: Throwable) -> Unit>()

    @Synchronized
    fun handle(exception: Throwable) {
        if (this.exception == null) {
            this.exception = exception
            log.error("An error occurred, shutting down NOW", exception)
            for (listener in listeners) {
                listener.invoke(exception)
            }
        } else {
            log.warn("Received exception but another exception was already caught", exception)
        }
    }

    fun register(listener: (exception: Throwable) -> Unit) {
        listeners.add(listener)
    }
}
