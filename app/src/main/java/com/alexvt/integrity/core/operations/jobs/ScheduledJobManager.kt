/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.jobs

import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.operations.snapshots.SnapshotOperationManager
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.metadata.SnapshotStatus
import com.alexvt.integrity.lib.core.operations.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

/**
 * Provides info about jobs on snapshots scheduled in the future,
 * and runs these jobs.
 */
abstract class ScheduledJobManager(
        protected val metadataRepository: MetadataRepository,
        protected val settingsRepository: SettingsRepository,
        private val snapshotOperationManager: SnapshotOperationManager,
        private val logger: Logger
) {

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
    private fun invokeListenersWithCurrentData() = GlobalScope.launch {
        val scheduledJobIds = withContext(Dispatchers.Default) {
            getScheduledJobsBlocking()
        }.map { Pair(it.artifactId, it.date) }
        scheduledJobsListenerMap.forEach {
            it.value.invoke(scheduledJobIds)
        }
    }

    /**
     * Gets list of snapshots scheduled to download next to scheduled timestamps.
     *
     * Sorted by time remaining until job starts.
     */
    private fun getScheduledJobsBlocking() = if (settingsRepository.get().jobsEnableScheduled) {
        metadataRepository.getAllArtifactLatestMetadataBlocking()
                .filter { it.downloadSchedule.periodSeconds > 0L } // todo also resume jobs interrupted not by user
                .sortedBy { getNextRunTimestamp(it) }
    } else {
        emptyList()
    }

    private fun getNextJobDelay(snapshot: Snapshot)
            = getNextRunTimestamp(snapshot) - System.currentTimeMillis()

    fun getNextRunTimestamp(snapshot: Snapshot)
            = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").parse(snapshot.date).time +
            snapshot.downloadSchedule.periodSeconds * 1000

    /**
     * Schedules jobs eligible for scheduling at the moment of calling
     * todo edit only changed jobs
     */
    abstract fun platformScheduleJobs(jobList: List<SnapshotDataDownloadScheduledJob>)

    fun updateSchedule() = GlobalScope.launch {
        getScheduledJobsBlocking().map {
            SnapshotDataDownloadScheduledJob(it.artifactId, it.date, it.title, getNextJobDelay(it))
        }.let {
            platformScheduleJobs(it)
            invokeListenersWithCurrentData()
            logger.log("Updated future jobs schedule: enabled = " +
                    "${settingsRepository.get().jobsEnableScheduled}, ${it.size} jobs scheduled")
        }
    }.let { /* no return type */ }

    /**
     * Invoke in subclasses when platform scheduler wait time expires for a snapshot.
     */
    protected fun startDownloadSnapshotData(artifactId: Long, date: String) {
        // todo Log(context, "Beginning scheduled job").snapshot(artifactId, date).build()

        // Starting creating snapshot async. Use RunningJobManager to build status
        val latestSnapshot = metadataRepository.getSnapshotMetadataBlocking(artifactId, date)
        snapshotOperationManager.saveSnapshot( // todo pass in constructor
                latestSnapshot.copy(status = SnapshotStatus.IN_PROGRESS))
    }

}
