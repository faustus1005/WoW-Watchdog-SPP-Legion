package com.wowwatchdog.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wowwatchdog.app.data.repository.ApiResult
import com.wowwatchdog.app.data.repository.WatchdogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class StatusPollingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WatchdogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (repository.getStatus()) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "status_polling"

        fun enqueue(context: Context, intervalMinutes: Long = 15) {
            val request = PeriodicWorkRequestBuilder<StatusPollingWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
