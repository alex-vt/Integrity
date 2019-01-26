/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.job

import android.content.Context
import androidx.work.*
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.IntegrityCore.context
import com.alexvt.integrity.lib.Log
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

/**
 * Provides info about jobs on snapshots scheduled in the future,
 * and runs these jobs.
 */
object ScheduledJobManager {

    private var scheduledJobsListenerMap: Map<String, ((List<Pair<Long, String>>) -> Unit)> = emptyMap()

    fun addScheduledJobsListener(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit) {
        if (scheduledJobsListenerMap.isEmpty()) {
            startJobStatusUpdateTimer()
        }
        scheduledJobsListenerMap = scheduledJobsListenerMap.plus(Pair(tag, jobsListener))
        invokeListenersWithCurrentData()
    }

    fun removeScheduledJobsListener(tag: String) {
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
        GlobalScope.launch (Dispatchers.Main) {
            scheduledJobsListenerMap.forEach {
                it.value.invoke(scheduledJobIds)
            }
        }
    }

    /**
     * Gets list of snapshots scheduled to download next to scheduled timestamps.
     *
     * Sorted by time remaining until job starts.
     */
    private fun getScheduledJobs()
            = IntegrityCore.metadataRepository.getAllArtifactLatestMetadata(false)
            .snapshots
            .filter { it.downloadSchedule.periodSeconds > 0L } // todo also resume jobs interrupted not by user
            .sortedBy { getNextRunTimestamp(it) }

    fun getNextRunTimestamp(snapshot: Snapshot)
            = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").parse(snapshot.date).time +
            snapshot.downloadSchedule.periodSeconds * 1000

    private fun getNextJobDelay(snapshot: Snapshot)
            = getNextRunTimestamp(snapshot) - System.currentTimeMillis()

    /**
     * Schedules jobs eligible for scheduling at the moment of calling
     * todo edit only changed jobs
     */
    fun updateSchedule(context: Context) {
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
        Log(context, "Updated future jobs schedule: ${workList.size} jobs scheduled").log()
    }

}

class SnapshotDownloadWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    override fun doWork(): Result {
        val artifactId = inputData.getLong("artifactId", -1)
        val date = inputData.getString("date")!!
        Log(context, "Beginning scheduled job").snapshot(artifactId, date).log()

        // Starting creating snapshot async. Use RunningJobManager to get status

        val latestSnapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(artifactId, date)
        IntegrityCore.saveSnapshot(context, latestSnapshot.copy(status = SnapshotStatus.IN_PROGRESS))

        return Result.success()
    }

}