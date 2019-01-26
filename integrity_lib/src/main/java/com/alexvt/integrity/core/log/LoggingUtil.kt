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

object LoggingUtil {

    /**
     * Shows event in logcat and sends it to the main app log repository.
     *
     * As the log repository can be in a different app, log entry is passed through a broadcast.
     */
    fun registerLogEvent(context: Context, logEntry: LogEntry) {
        showLogEntryInLogcat(logEntry)
        sendLogEntryBroadcast(context, logEntry, logEntry.type == LogEntryType.CRASH)
    }

    private fun showLogEntryInLogcat(logEntry: LogEntry) {
        if (logEntry.type == LogEntryType.ERROR || logEntry.type == LogEntryType.CRASH) {
            android.util.Log.e(logEntry.tag, logEntry.text
                    + "\n" + logEntry.stackTraceText + "\n" + logEntry.data.toString())
        } else {
            android.util.Log.d(logEntry.tag, logEntry.text + "\n" + logEntry.data.toString())
        }
    }

    private fun sendLogEntryBroadcast(context: Context, logEntry: LogEntry,
                                      useRecoveryProcess: Boolean) {
        context.applicationContext.sendBroadcast(IntentUtil.withLogEntry(logEntry).apply {
            action = if (useRecoveryProcess) {
                "com.alexvt.integrity.CRASH_RECOVERY"
            } else {
                "com.alexvt.integrity.LOG_ENTRY_ADDED"
            }
        })
    }

    // Broadcast receiver for receiving status updates from the IntentService.
    class LogEntryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            IntegrityCore.logRepository.addEntry(IntentUtil.getLogEntry(intent))
            IntegrityCore.notifyAboutUnreadErrors(context)
        }
    }

    // Using a broadcast receiver in a temporary separate recovery process
    // to receive crash log entry from the faulty process (which then is terminated)
    // and sending it to the main process (starts again if it was terminated or not running).
    class CrashRecoveryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            android.util.Log.v(LoggingUtil::class.java.simpleName,
                    "CrashRecoveryReceiver in the recovery process is re-broadcasting " +
                            "crash log entry to the (newly created if needed) main app process...")
            sendLogEntryBroadcast(context, IntentUtil.getLogEntry(intent), false)
            Runtime.getRuntime().exit(0) // recovery process not needed anymore
        }
    }
}