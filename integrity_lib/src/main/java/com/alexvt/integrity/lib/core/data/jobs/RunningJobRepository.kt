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
interface RunningJobRepository {

    fun putJob(snapshot: Snapshot)

    fun isRunning(snapshot: Snapshot) = isRunning(snapshot.artifactId, snapshot.date)

    fun isRunning(artifactId: Long, date: String): Boolean

    fun setJobProgressListener(snapshot: Snapshot,
                               progressListener: (JobProgress<Snapshot>) -> Unit)

    fun invokeJobProgressListener(snapshot: Snapshot,
                                  progress: JobProgress<Snapshot>)

    fun markJobCanceled(artifactId: Long, date: String)

    fun markJobCanceled(snapshot: Snapshot) = markJobCanceled(snapshot.artifactId, snapshot.date)

    fun removeJob(snapshot: Snapshot)

    fun addJobListListener(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit)

    fun removeJobListListener(tag: String)
}