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
object LongRunningJobManager {

    var coroutineJobMap: Map<Long, Job> = mapOf()

    fun addJob(id: Long, coroutineJob: Job) {
        coroutineJobMap = coroutineJobMap.plus(Pair(id, coroutineJob))
    }

    fun cancelJob(id: Long) {
        coroutineJobMap[id]?.cancel()
        removeJob(coroutineJobMap[id])
    }

    fun removeJob(coroutineJob: CoroutineContext?) {
        coroutineJobMap = coroutineJobMap.filterValues { it != coroutineJob }
    }

}