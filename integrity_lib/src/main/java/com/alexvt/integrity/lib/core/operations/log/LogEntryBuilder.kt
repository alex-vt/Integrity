/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.log

import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.core.data.log.LogEntryType
import com.alexvt.integrity.lib.core.data.log.LogKey
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat

class LogEntryBuilder(private val text: String) {

    private var type = LogEntryType.NORMAL
    private var throwable: Throwable? = null
    private val data: LinkedHashMap<String, String> = linkedMapOf()

    fun snapshot(snapshot: Snapshot?) = snapshot(snapshot?.artifactId, snapshot?.date)

    fun snapshot(artifactId: Long?, date: String?): LogEntryBuilder {
        if (artifactId != null) data[LogKey.ARTIFACT_ID] = "" + artifactId
        if (date != null) data[LogKey.SNAPSHOT_DATE] = date
        return this
    }

    fun thread(thread: Thread): LogEntryBuilder {
        data[LogKey.THREAD] = thread.toString()
        return this
    }

    fun throwable(throwable: Throwable?): LogEntryBuilder {
        this.throwable = throwable
        return this
    }

    fun type(type: String): LogEntryBuilder {
        this.type = type
        return this
    }

    fun packageName(packageName: String): LogEntryBuilder {
        data[LogKey.PACKAGE] = packageName
        return this
    }

    fun process(process: String): LogEntryBuilder {
        data[LogKey.PROCESS] = process
        return this
    }

    fun build(): LogEntry {
        val currentThread = Thread.currentThread()
        val logEntryTime = getCurrentTimeText()
        return LogEntry(logEntryTime + getOrderIdSuffix(),
                logEntryTime, getTag(throwable, currentThread), text, data,
                getStackTraceText(throwable, currentThread), type)
    }


    private companion object {
        var counter = 0 // for maintaining addition order of build entries of the same timestamp.
    }

    private fun getOrderIdSuffix() = "-${counter++}"

    private fun getCurrentTimeText() = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
            .format(System.currentTimeMillis())


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

}