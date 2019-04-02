/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.operations.jobs

import android.content.Context
import androidx.work.*
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.operations.snapshots.SnapshotOperationManager
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.operations.jobs.ScheduledJobManager
import com.alexvt.integrity.core.operations.jobs.SnapshotDataDownloadScheduledJob
import com.alexvt.integrity.lib.core.operations.log.LogManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Provides info about jobs on snapshots scheduled in the future,
 * and runs these jobs.
 */
class AndroidScheduledJobManager @Inject constructor(
        metadataRepository: MetadataRepository,
        settingsRepository: SettingsRepository,
        snapshotOperationManager: SnapshotOperationManager,
        logManager: LogManager
) : ScheduledJobManager(metadataRepository, settingsRepository, snapshotOperationManager, logManager) {

    /**
     * Schedules jobs eligible for scheduling at the moment of calling
     * todo edit only changed jobs
     */
    override fun platformScheduleJobs(jobList: List<SnapshotDataDownloadScheduledJob>) {
        val workManagerJobTag = "downloading"
        WorkManager.getInstance().pruneWork()
        WorkManager.getInstance().cancelAllWorkByTag(workManagerJobTag)
        jobList.map {
            android.util.Log.v(ScheduledJobManager::class.java.simpleName, "Scheduling download job " +
                    "in ${it.startDelayMillis} ms for ${it.title} (artifactID ${it.artifactId})")
            OneTimeWorkRequest.Builder(SnapshotDownloadWorker::class.java)
                    .setInitialDelay(it.startDelayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder()
                            .putLong("artifactId", it.artifactId)
                            .putString("date", it.date)
                            .build())
                    .addTag(workManagerJobTag)
                    .build()
        }.let {
            if (jobList.isNotEmpty()) {
                WorkManager.getInstance().enqueue(it)
            }
        }
    }

    inner class SnapshotDownloadWorker(val context: Context, params: WorkerParameters): Worker(context, params) {
        // todo fix DI, inject context
        override fun doWork(): Result {
            val artifactId = inputData.getLong("artifactId", -1)
            val date = inputData.getString("date")!!

            startDownloadSnapshotData(artifactId, date)

            return Result.success()
        }
    }

}