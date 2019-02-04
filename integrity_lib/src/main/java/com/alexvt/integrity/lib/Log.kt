/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib

import android.content.Context
import com.alexvt.integrity.core.log.LogEntry
import com.alexvt.integrity.core.log.LogEntryType
import com.alexvt.integrity.core.log.LogKey
import com.alexvt.integrity.core.log.LoggingUtil
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import android.app.ActivityManager


/**
 * Logger (builder-like).
 */
class Log(val context: Context, val text: String) {

    private val data: LinkedHashMap<String, String> = linkedMapOf()

    fun snapshot(snapshot: Snapshot) = snapshot(snapshot.artifactId, snapshot.date)

    fun snapshot(artifactId: Long, date: String): Log {
        data[LogKey.ARTIFACT_ID] = "" + artifactId
        data[LogKey.SNAPSHOT_DATE] = date
        return this
    }

    fun thread(thread: Thread): Log {
        data[LogKey.THREAD] = thread.toString()
        return this
    }

    fun logDebug() {
        log(LogEntryType.DEBUG)
    }

    fun log() {
        log(LogEntryType.NORMAL)
    }

    fun logError(throwable: Throwable? = null) {
        log(LogEntryType.ERROR, throwable)
    }

    fun logCrash(throwable: Throwable) {
        log(LogEntryType.CRASH, throwable)
    }

    private fun log(type: String) {
        log(type, null)
    }

    private fun log(logEntryType: String, throwable: Throwable?) {
        val currentThread = Thread.currentThread()
        val fullData = addMoreLogData(context, data, currentThread)
        val logEntryTime = getCurrentTimeText()
        LoggingUtil.registerLogEvent(context, LogEntry(logEntryTime + getOrderIdSuffix(),
                logEntryTime, getTag(throwable, currentThread), text, fullData,
                getStackTraceText(throwable, currentThread), logEntryType))
    }

    private companion object {
        var counter = 0 // for maintaining addition order of log entries of the same timestamp.
    }

    private fun getOrderIdSuffix() = "-${counter++}"

    private fun getCurrentTimeText() = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
            .format(System.currentTimeMillis())

    private fun addMoreLogData(context: Context, data: LinkedHashMap<String, String>,
                               currentThread: Thread): LinkedHashMap<String, String> {
        data[LogKey.PACKAGE] = context.packageName
        if (!data.containsKey(LogKey.THREAD)) {
            data[LogKey.THREAD] = currentThread.toString()
        }
        data[LogKey.PROCESS] = getCurrentProcessInfo(context)
        return data
    }

    private val threadStackTraceSkip: Int = 6 // skipping the top of the stack here in Log

    private fun getStackTraceText(throwable: Throwable?, currentThread: Thread): String {
        val writer = StringWriter()
        if (throwable != null) {
            throwable.printStackTrace(PrintWriter(writer))
        } else {
            val stackTraces = currentThread.stackTrace
            for (i in threadStackTraceSkip until stackTraces.size) {
                PrintWriter(writer).println("\tat " + stackTraces[i])
            }
        }
        return writer.toString().trimEnd()
    }

    private fun getTag(throwable: Throwable?, currentThread: Thread): String {
        var className = "LogEntry-default"
        if (throwable != null) {
            className = throwable.stackTrace[0].className
        } else if (currentThread.stackTrace.size > threadStackTraceSkip) {
            className = currentThread.stackTrace[threadStackTraceSkip].className
        }
        return className.substringAfterLast(".").substringBefore("$")
    }

    private fun getCurrentProcessInfo(context: Context): String {
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
}