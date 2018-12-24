/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.job

import android.content.Context
import android.util.Log
import androidx.work.*
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * Provides info about jobs on snapshots scheduled in the future,
 * and runs these jobs.
 */
object ScheduledJobManager {

    /**
     * Gets map of snapshots scheduled to download next to scheduled timestamps.
     * Currently running jobs are excluded.
     */
    fun getUpcomingJobMap()
            = IntegrityCore.metadataRepository.getAllArtifactLatestMetadata(false)
            .snapshotMetadataList
            .filter { it.downloadSchedule.periodSeconds > 0L } // todo also resume jobs interrupted not by user
            .filterNot { RunningJobManager.isRunning(it) }
            .associate { it to getNextJobTimestamp(it) }

    private fun getNextJobTimestamp(snapshotMetadata: SnapshotMetadata)
            = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").parse(snapshotMetadata.date).time +
            snapshotMetadata.downloadSchedule.periodSeconds * 1000

    private fun getNextJobDelay(snapshotMetadata: SnapshotMetadata)
            = getNextJobTimestamp(snapshotMetadata) - System.currentTimeMillis()

    /**
     * Schedules jobs eligible to scheduling at the moment of calling
     * todo edit only changed jobs
     */
    fun updateSchedule() {
        Log.d(ScheduledJobManager::class.java.simpleName, "Updating schedule...")
        val workManagerJobTag = "downloading"
        WorkManager.getInstance().pruneWork()
        WorkManager.getInstance().cancelAllWorkByTag(workManagerJobTag)
        val workList = getUpcomingJobMap().keys.map {
            Log.d(ScheduledJobManager::class.java.simpleName, "Scheduling download job " +
                    "in ${getNextJobDelay(it)} ms for ${it.title} (artifactID ${it.artifactId})")
            OneTimeWorkRequest.Builder(SnapshotDownloadWorker::class.java)
                    .setInitialDelay(getNextJobDelay(it), TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder()
                            .putLong("artifactId", it.artifactId)
                            .putString("date", it.date)
                            .build())
                    .addTag(workManagerJobTag)
                    .build()
        }
        if (workList.isNotEmpty()) {
            WorkManager.getInstance().enqueue(workList)
        }
        Log.d(ScheduledJobManager::class.java.simpleName, "Updated schedule: " +
                "${workList.size} jobs added")
    }

}

class SnapshotDownloadWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    override fun doWork(): Result {
        val artifactId = inputData.getLong("artifactId", -1)
        val date = inputData.getString("date")!!
        Log.d(ScheduledJobManager::class.java.simpleName, "Beginning scheduled job")
        // Starting creating snapshot async. Use RunningJobManager to get status

        val latestSnapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(artifactId, date)
        val blueprintDate = IntegrityCore.saveSnapshotBlueprint(latestSnapshot)
        IntegrityCore.createSnapshotFromBlueprint(artifactId, blueprintDate)

        return Result.success()
    }

}