/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.IntegrityCore
import javax.inject.Inject

class ScheduledJobsEnableReceiver : BroadcastReceiver() {
    @Inject
    lateinit var integrityCore: IntegrityCore
    override fun onReceive(context: Context, intent: Intent) {
        integrityCore.updateScheduledJobsOptions(true)
    }
}