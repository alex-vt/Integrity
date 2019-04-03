/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.data.jobs

import com.alexvt.integrity.lib.core.data.metadata.Snapshot

/**
 * Allows accounting jobs by given corresponding snapshot,
 * registering job progress listener,
 * and invoking now progress listeners for existing jobs with recent progress.
 *
 * Jobs can be marked canceled and other components can check if job is canceled.
 */
class InMemoryRunningJobRepository : RunningJobRepository {

    private val tag = InMemoryRunningJobRepository::class.java.simpleName

    private var jobStatusMap: Map<String, Boolean> = mapOf() // values is true when job running
    private var jobProgressListenerMap: Map<String, ((JobProgress<Snapshot>) -> Unit)> = mapOf()
    private var recentJobProgressMap: Map<String, JobProgress<Snapshot>> = mapOf()

    override fun putJob(snapshot: Snapshot) {
        jobStatusMap = jobStatusMap.minus(getId(snapshot)) // replacing
        jobStatusMap = jobStatusMap.plus(Pair(getId(snapshot), true))
        invokeJobListListeners()
    }

    override fun isRunning(artifactId: Long, date: String): Boolean {
        val isRunning = jobStatusMap.containsKey(getId(artifactId, date))
                && jobStatusMap[getId(artifactId, date)] == true
        return isRunning
    }

    override fun setJobProgressListener(snapshot: Snapshot,
                                        progressListener: (JobProgress<Snapshot>) -> Unit) {
        jobProgressListenerMap = jobProgressListenerMap
                .plus(Pair(getId(snapshot), progressListener))
        invokeWithRecentProgress(snapshot)
    }

    override fun invokeJobProgressListener(snapshot: Snapshot,
                                           progress: JobProgress<Snapshot>) {
        jobProgressListenerMap[getId(snapshot)]?.invoke(progress)
        recentJobProgressMap = recentJobProgressMap
                .plus(Pair(getId(snapshot), progress))
    }

    override fun markJobCanceled(artifactId: Long, date: String) {
        jobStatusMap = jobStatusMap.minus(getId(artifactId, date))
        invokeJobListListeners()
    }

    override fun removeJob(snapshot: Snapshot) {
        jobStatusMap = jobStatusMap.minus(getId(snapshot))
        invokeJobListListeners()
        jobProgressListenerMap = jobProgressListenerMap.minus(getId(snapshot))
        recentJobProgressMap = recentJobProgressMap.minus(getId(snapshot))
    }


    private var runningJobsListenerMap: Map<String, ((List<Pair<Long, String>>) -> Unit)> = emptyMap()

    override fun addJobListListener(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit) {
        runningJobsListenerMap = runningJobsListenerMap.plus(Pair(tag, jobsListener))
        invokeJobListListeners()
    }

    override fun removeJobListListener(tag: String) {
        runningJobsListenerMap = runningJobsListenerMap.minus(tag)
    }

    /**
     * Feeds listeners with scheduled jobs at the moment
     */
    private fun invokeJobListListeners() {
        val runningJobSnapshotIds = jobStatusMap
                .filter { it.value } // not canceled
                .map { getArtifactIdAndDate(it.key) }
        runningJobsListenerMap.forEach {
            it.value.invoke(runningJobSnapshotIds)
        }
    }


    private fun invokeWithRecentProgress(snapshot: Snapshot) {
        if (recentJobProgressMap.containsKey(getId(snapshot))) {
            invokeJobProgressListener(snapshot, recentJobProgressMap[getId(snapshot)]!!)
        }
    }

    /**
     * Unique snapshot ID based on artifact ID and date is used as map key.
     */
    private fun getId(snapshot: Snapshot) = getId(snapshot.artifactId, snapshot.date)

    private fun getId(artifactId: Long, date: String) = "" + artifactId + "_" + date


    /**
     * Reversing unique snapshot ID based on artifact ID and date to artifact ID and date
     */
    private fun getArtifactIdAndDate(uniqueId: String) = Pair(
            uniqueId.substringBefore('_').toLong(),
            uniqueId.substringAfter('_')
    )
}