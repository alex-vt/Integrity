/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.log.LogEntry
import com.alexvt.integrity.lib.util.IntentUtil
import dagger.android.AndroidInjection
import javax.inject.Inject

object LogEventReceiver {

    // Broadcast receiver for receiving status updates from the IntentService.
    class LogEntryReceiver : BroadcastReceiver() {
        @Inject
        lateinit var integrityCore: IntegrityCore

        override fun onReceive(context: Context, intent: Intent) {
            AndroidInjection.inject(this, context)
            integrityCore.logRepository.addEntry(IntentUtil.getLogEntry(intent))
        }
    }

    class LogReadReceiver : BroadcastReceiver() {
        @Inject
        lateinit var integrityCore: IntegrityCore

        override fun onReceive(context: Context, intent: Intent) {
            AndroidInjection.inject(this, context)
            integrityCore.markErrorsRead(context)
        }
    }

}