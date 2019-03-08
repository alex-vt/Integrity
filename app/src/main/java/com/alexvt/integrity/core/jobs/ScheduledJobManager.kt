/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.jobs

import android.content.Context
import com.alexvt.integrity.lib.metadata.Snapshot
import java.text.SimpleDateFormat

/**
 * Provides info about jobs on snapshots scheduled in the future,
 * and runs these jobs.
 */
interface ScheduledJobManager {

    fun addScheduledJobsListener(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit)

    fun removeScheduledJobsListener(tag: String)

    fun getNextRunTimestamp(snapshot: Snapshot)
            = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").parse(snapshot.date).time +
            snapshot.downloadSchedule.periodSeconds * 1000

    /**
     * Schedules jobs eligible for scheduling at the moment of calling
     * todo edit only changed jobs
     */
    fun updateSchedule(context: Context)

}
