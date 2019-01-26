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
class Log(val context: Context) {

    private val data: LinkedHashMap<String, String> = linkedMapOf()

    fun what(what: String): Log {
        data[LogKey.DESCRIPTION] = what
        return this
    }

    fun where(method: String) = where(context.javaClass.name, method)

    fun where(inObject: Any, method: String) = where(inObject.javaClass.name, method)

    private fun where(className: String, methodName: String): Log {
        data[LogKey.CLASS] = className
        data[LogKey.METHOD] = methodName
        return this
    }

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
        val fullData = addMoreLogData(context, data, throwable)
        val logEntryTime = getCurrentTimeText()
        LoggingUtil.registerLogEvent(context, LogEntry(logEntryTime + getOrderIdSuffix(),
                logEntryTime, fullData, logEntryType))
    }

    private companion object {
        var counter = 0 // for maintaining addition order of log entries of the same timestamp.
    }

    private fun getOrderIdSuffix() = "-${counter++}"

    private fun getCurrentTimeText() = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
            .format(System.currentTimeMillis())

    private fun addMoreLogData(context: Context, data: LinkedHashMap<String, String>,
                               throwable: Throwable?): LinkedHashMap<String, String> {
        if (throwable != null) {
            data[LogKey.STACK_TRACE] = getStackTrace(throwable)
        }
        data[LogKey.PACKAGE] = context.packageName
        if (!data.containsKey(LogKey.THREAD)) {
            data[LogKey.THREAD] = Thread.currentThread().toString()
        }
        data[LogKey.PROCESS] = getCurrentProcessInfo(context)
        return data
    }

    private fun getStackTrace(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
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