package utils

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Scheduler(private val task: Runnable, private val initDelay: Long = 5000L) {

    private val executor = Executors.newScheduledThreadPool(1)

    fun schedule(every: Every) {
        val taskWrapper = Runnable { task.run() }
        executor.scheduleWithFixedDelay(taskWrapper, initDelay, every.n, every.unit)
    }

    fun stop() {
        executor.shutdown()
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: Exception) {}
    }
}