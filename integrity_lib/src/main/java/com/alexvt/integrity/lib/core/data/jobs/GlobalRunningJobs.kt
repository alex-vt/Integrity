/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.data.jobs

import com.alexvt.integrity.lib.core.data.jobs.InMemoryRunningJobRepository
import com.alexvt.integrity.lib.core.data.jobs.RunningJobRepository

object GlobalRunningJobs {

    val RUNNING_JOB_REPOSITORY: RunningJobRepository by lazy { InMemoryRunningJobRepository() }

}