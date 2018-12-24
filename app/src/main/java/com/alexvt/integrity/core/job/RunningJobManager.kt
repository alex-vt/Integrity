/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.job

import com.alexvt.integrity.core.SnapshotMetadata
import kotlinx.coroutines.Job

/**
 * Allows canceling coroutine jobs by given corresponding SnapshotMetadata,
 * registering job progress listener,
 * and invoking now progress listeners for existing jobs with recent progress.
 */
object RunningJobManager {

    var coroutineJobMap: Map<String, Job> = mapOf()
    var jobProgressListenerMap: Map<String, ((JobProgress<SnapshotMetadata>) -> Unit)> = mapOf()
    var recentJobProgressMap: Map<String, JobProgress<SnapshotMetadata>> = mapOf()

    fun addJob(snapshotMetadata: SnapshotMetadata, coroutineJob: Job) {
        coroutineJobMap = coroutineJobMap
                .plus(Pair(getId(snapshotMetadata), coroutineJob))
    }

    fun isRunning(snapshotMetadata: SnapshotMetadata)
            = coroutineJobMap.containsKey(getId(snapshotMetadata))

    fun setJobProgressListener(snapshotMetadata: SnapshotMetadata,
                               progressListener: (JobProgress<SnapshotMetadata>) -> Unit) {
        jobProgressListenerMap = jobProgressListenerMap
                .plus(Pair(getId(snapshotMetadata), progressListener))
        invokeWithRecentProgress(snapshotMetadata)
    }

    fun invokeJobProgressListener(snapshotMetadata: SnapshotMetadata,
                                  progress: JobProgress<SnapshotMetadata>) {
        jobProgressListenerMap[getId(snapshotMetadata)]?.invoke(progress)
        recentJobProgressMap = recentJobProgressMap
                .plus(Pair(getId(snapshotMetadata), progress))
    }

    fun removeJob(snapshotMetadata: SnapshotMetadata) {
        coroutineJobMap[getId(snapshotMetadata)]?.cancel()
        coroutineJobMap = coroutineJobMap.minus(getId(snapshotMetadata))
        jobProgressListenerMap = jobProgressListenerMap.minus(getId(snapshotMetadata))
        recentJobProgressMap = recentJobProgressMap.minus(getId(snapshotMetadata))
    }


    private fun invokeWithRecentProgress(snapshotMetadata: SnapshotMetadata) {
        if (recentJobProgressMap.containsKey(getId(snapshotMetadata))) {
            invokeJobProgressListener(snapshotMetadata, recentJobProgressMap[getId(snapshotMetadata)]!!)
        }
    }

    /**
     * Unique snapshot ID based on artifact ID and date is used as map key.
     */
    private fun getId(snapshotMetadata: SnapshotMetadata)
            = "" + snapshotMetadata.artifactId + "_" + snapshotMetadata.date

}