/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.jobs

import android.content.Context
import androidx.work.*
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.operations.SnapshotOperationManager
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.log.Log
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.concurrent.schedule

/**
 * Provides info about jobs on snapshots scheduled in the future,
 * and runs these jobs.
 */
class AndroidScheduledJobManager @Inject constructor(
        private val metadataRepository: MetadataRepository,
        private val settingsRepository: SettingsRepository,
        private val snapshotOperationManager: SnapshotOperationManager
) : ScheduledJobManager {

    private var scheduledJobsListenerMap: Map<String, ((List<Pair<Long, String>>) -> Unit)> = emptyMap()

    override fun addScheduledJobsListener(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit) {
        if (scheduledJobsListenerMap.isEmpty()) {
            startJobStatusUpdateTimer()
        }
        scheduledJobsListenerMap = scheduledJobsListenerMap.plus(Pair(tag, jobsListener))
        invokeListenersWithCurrentData()
    }

    override fun removeScheduledJobsListener(tag: String) {
        scheduledJobsListenerMap = scheduledJobsListenerMap.minus(tag)
        if (scheduledJobsListenerMap.isEmpty()) {
            stopJobStatusUpdateTimer()
        }
    }

    private lateinit var jobsStatusUpdateTimer: TimerTask // todo extract to universal timer

    private fun startJobStatusUpdateTimer() {
        val statusPollingPeriodMillis = 2000L
        jobsStatusUpdateTimer = Timer().schedule(delay = statusPollingPeriodMillis,
                period = statusPollingPeriodMillis) {
            invokeListenersWithCurrentData()
        }
    }

    private fun stopJobStatusUpdateTimer() {
        if (!::jobsStatusUpdateTimer.isInitialized) {
            return
        }
        jobsStatusUpdateTimer.cancel()
    }

    /**
     * Feeds listeners with artifactIDs and dates of scheduled jobs at the moment
     */
    private fun invokeListenersWithCurrentData() {
        val scheduledJobIds = getScheduledJobs().map { Pair(it.artifactId, it.date) }
        scheduledJobsListenerMap.forEach {
            it.value.invoke(scheduledJobIds)
        }
    }

    /**
     * Gets list of snapshots scheduled to download next to scheduled timestamps.
     *
     * Sorted by time remaining until job starts.
     */
    private fun getScheduledJobs() = if (settingsRepository.get().jobsEnableScheduled) {
        metadataRepository.getAllArtifactLatestMetadataBlocking(false)
                .filter { it.downloadSchedule.periodSeconds > 0L } // todo also resume jobs interrupted not by user
                .sortedBy { getNextRunTimestamp(it) }
    } else {
        emptyList()
    }

    private fun getNextJobDelay(snapshot: Snapshot)
            = getNextRunTimestamp(snapshot) - System.currentTimeMillis()

    /**
     * Schedules jobs eligible for scheduling at the moment of calling
     * todo edit only changed jobs
     */
    override fun updateSchedule(context: Context) {
        val workManagerJobTag = "downloading"
        WorkManager.getInstance().pruneWork()
        WorkManager.getInstance().cancelAllWorkByTag(workManagerJobTag)
        val workList = getScheduledJobs().map {
            android.util.Log.v(ScheduledJobManager::class.java.simpleName, "Scheduling download job " +
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
        invokeListenersWithCurrentData()
        Log(context, "Updated future jobs schedule: enabled = " +
                "${settingsRepository.get().jobsEnableScheduled}, ${workList.size} jobs scheduled").log()
    }

    inner class SnapshotDownloadWorker(val context: Context, params: WorkerParameters): Worker(context, params) {
        override fun doWork(): Result {
            val artifactId = inputData.getLong("artifactId", -1)
            val date = inputData.getString("date")!!
            Log(context, "Beginning scheduled job").snapshot(artifactId, date).log()

            // Starting creating snapshot async. Use RunningJobManager to get status

            val latestSnapshot = metadataRepository.getSnapshotMetadataBlocking(artifactId, date)
            snapshotOperationManager.saveSnapshot( // todo pass in constructor
                    latestSnapshot.copy(status = SnapshotStatus.IN_PROGRESS))

            return Result.success()
        }
    }

}