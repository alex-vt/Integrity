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
import com.alexvt.integrity.lib.util.IntentUtil

object LogEventReceiver {
    // Broadcast receiver for receiving status updates from the IntentService.
    class LogEntryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            acceptLogEntry(context, IntentUtil.getLogEntry(intent))
        }
    }


    private fun acceptLogEntry(context: Context, logEntry: LogEntry) {
        IntegrityCore.logRepository.addEntry(context, logEntry)
    }

    class LogReadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            IntegrityCore.markErrorsRead(context)
        }
    }

}