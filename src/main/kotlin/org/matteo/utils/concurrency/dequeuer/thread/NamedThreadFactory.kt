package org.matteo.utils.concurrency.dequeuer.thread

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(private val name: String) : ThreadFactory {
    private val counter = AtomicInteger()

    override fun newThread(runnable: Runnable): Thread {
        return Thread(runnable, String.format("%s-%d", name, counter.incrementAndGet()))
    }
}
