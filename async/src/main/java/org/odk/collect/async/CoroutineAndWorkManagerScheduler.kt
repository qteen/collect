package org.odk.collect.async

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class CoroutineAndWorkManagerScheduler(foregroundContext: CoroutineContext, backgroundContext: CoroutineContext, private val workManager: WorkManager) : CoroutineScheduler(foregroundContext, backgroundContext) {

    constructor(workManager: WorkManager) : this(Dispatchers.Main, Dispatchers.IO, workManager) // Needed for Java construction

    override fun networkDeferred(tag: String, spec: TaskSpec) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequest.Builder(spec.getWorkManagerAdapter())
            .addTag(tag)
            .setConstraints(constraints)
            .build()

        workManager.beginUniqueWork(tag, ExistingWorkPolicy.KEEP, workRequest).enqueue()
    }

    override fun networkDeferred(tag: String, spec: TaskSpec, repeatPeriod: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val worker = spec.getWorkManagerAdapter()
        val workRequest = PeriodicWorkRequest.Builder(worker, repeatPeriod, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.REPLACE, workRequest)
    }

    override fun cancelDeferred(tag: String) {
        workManager.cancelUniqueWork(tag)
    }

    override fun isRunning(tag: String): Boolean {
        return isWorkManagerWorkRunning(tag)
    }

    private fun isWorkManagerWorkRunning(tag: String): Boolean {
        val statuses = workManager.getWorkInfosByTag(tag)
        for (workInfo in statuses.get()) {
            if (workInfo.state == WorkInfo.State.RUNNING) {
                return true
            }
        }

        return false
    }
}
