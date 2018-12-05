/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.job

import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Allows canceling coroutine jobs by given assigned ID.
 */
object CoroutineJobManager {

    var coroutineJobMap: Map<Long, Job> = mapOf()

    fun addJob(coroutineJob: Job): Long {
        val jobId = System.currentTimeMillis() + coroutineJob.hashCode()
        coroutineJobMap = coroutineJobMap.plus(Pair(jobId, coroutineJob))
        return jobId
    }

    fun cancelJob(jobId: Long) {
        coroutineJobMap[jobId]?.cancel()
        removeJob(coroutineJobMap[jobId])
    }

    fun removeJob(coroutineJob: CoroutineContext?) {
        coroutineJobMap = coroutineJobMap.filterValues { it != coroutineJob }
    }

}