/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.IntentUtil

object LoggingUtil {

    /**
     * Shows event in logcat and sends it to the main app log repository.
     *
     * As the log repository can be in a different app, log entry is passed through a broadcast.
     */
    fun registerLogEvent(context: Context, logEntry: LogEntry) {
        showLogEntryInLogcat(logEntry)
        sendLogEntryBroadcast(context, logEntry)
    }

    private fun showLogEntryInLogcat(logEntry: LogEntry) {
        val entryClassName = logEntry.data[LogKey.CLASS]
        val defaultClassName = LoggingUtil::class.java.name
        val tag = (entryClassName ?: "$defaultClassName-default").substringAfterLast(".")
        val text = logEntry.data.toString() // todo format better

        if (logEntry.type == LogEntryType.ERROR || logEntry.type == LogEntryType.CRASH) {
            Log.e(tag, text)
        } else {
            Log.d(tag, text)
        }
    }

    private fun sendLogEntryBroadcast(context: Context, logEntry: LogEntry) {
        context.applicationContext.sendBroadcast(IntentUtil.withLogEntry(logEntry).apply {
            action = "com.alexvt.integrity.LOG_ENTRY_ADDED"
        })
    }

    // Broadcast receiver for receiving status updates from the IntentService.
    class LogEntryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            IntegrityCore.logRepository.addEntry(IntentUtil.getLogEntry(intent))
        }
    }
}