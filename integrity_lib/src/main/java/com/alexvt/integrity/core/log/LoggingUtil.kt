/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.IntentUtil

object LoggingUtil {

    /**
     * Shows event in logcat and sends it to the main app log repository.
     *
     * When the log entry cannot be written (crash, or not the main app),
     * it's passed through a broadcast.
     */
    fun registerLogEvent(context: Context, logEntry: LogEntry) {
        showLogEntryInLogcat(logEntry)
        if (canLogDirectly(context, logEntry)) {
            receiveLogEntry(context, logEntry)
        } else {
            sendLogEntryBroadcast(context, logEntry, isProcessFailed(logEntry))
        }
    }

    private fun canLogDirectly(context: Context, logEntry: LogEntry)
            = !isProcessFailed(logEntry) && isMainApp(context)

    private fun isProcessFailed(logEntry: LogEntry) = logEntry.type == LogEntryType.CRASH

    private fun isMainApp(context: Context): Boolean {
        val currentPid = android.os.Process.myPid()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = manager.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (processInfo.pid == currentPid && processInfo.processName == "com.alexvt.integrity") {
                    return true
                }
            }
        }
        return false
    }

    private fun showLogEntryInLogcat(logEntry: LogEntry) {
        if (logEntry.type == LogEntryType.ERROR || logEntry.type == LogEntryType.CRASH) {
            android.util.Log.e(logEntry.tag, logEntry.text
                    + "\n" + logEntry.stackTraceText + "\n" + logEntry.data.toString())
        } else {
            android.util.Log.d(logEntry.tag, logEntry.text + "\n" + logEntry.data.toString())
        }
    }

    fun logErrorInLogcat(text: String, t: Throwable) {
        android.util.Log.e("Log-Failure", text, t)
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

    private fun receiveLogEntry(context: Context, logEntry: LogEntry) {
        IntegrityCore.logRepository.addEntry(logEntry)
        IntegrityCore.notifyAboutUnreadErrors(context)
    }

    // Broadcast receiver for receiving status updates from the IntentService.
    class LogEntryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            receiveLogEntry(context, IntentUtil.getLogEntry(intent))
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

    class LogReadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            IntegrityCore.markErrorsRead(context)
        }
    }
}