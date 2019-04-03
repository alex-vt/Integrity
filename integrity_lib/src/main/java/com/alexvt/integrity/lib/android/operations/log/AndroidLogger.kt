/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.operations.log

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.core.data.log.LogEntryType
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.lib.core.operations.log.Logger

class AndroidLogger(private val context: Context) : Logger() {

    override fun printInConsole(logEntry: LogEntry) {
        if (logEntry.type == LogEntryType.ERROR || logEntry.type == LogEntryType.CRASH) {
            android.util.Log.e(logEntry.tag, logEntry.text
                    + "\n" + logEntry.stackTraceText + "\n" + logEntry.data.toString())
        } else if (logEntry.type == LogEntryType.NORMAL) {
            android.util.Log.d(logEntry.tag, logEntry.text + "\n" + logEntry.data.toString())
        } else {
            android.util.Log.v(logEntry.tag, logEntry.text + "\n" + logEntry.data.toString())
        }
    }

    override fun writeToLog(logEntry: LogEntry, recoverFromCrash: Boolean) {
        writeToLog(context, logEntry, recoverFromCrash)
    }

    override fun getPackageName() = context.packageName

    override fun getProcessName(): String {
        val pid = android.os.Process.myPid()
        val processIdInfoSuffix = " (process ID: $pid)"
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == pid) {
                return processInfo.processName + processIdInfoSuffix
            }
        }
        return "(no name)$processIdInfoSuffix"
    }

    private companion object {
        fun writeToLog(context: Context, logEntry: LogEntry, recoverFromCrash: Boolean) {
            context.applicationContext.sendBroadcast(IntentUtil.withLogEntry(logEntry).apply {
                action = if (recoverFromCrash) {
                    "com.alexvt.integrity.CRASH_RECOVERY"
                } else {
                    "com.alexvt.integrity.LOG_ENTRY_ADDED"
                }
            })
        }
    }

    // Using a broadcast receiver in a temporary separate recovery process
    // to receive crash build entry from the faulty process (which then is terminated)
    // and sending it to the main process (starts again if it was terminated or not running).
    inner class LogEntryRecoveryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            android.util.Log.e(AndroidLogger::class.java.simpleName,
                    "LogEntryRecoveryReceiver in the recovery process is re-broadcasting " +
                            "crash build entry to the (newly created if needed) main app process...")
            writeToLog(context, IntentUtil.getLogEntry(intent), false)
            Runtime.getRuntime().exit(0) // recovery process not needed anymore
        }
    }

}