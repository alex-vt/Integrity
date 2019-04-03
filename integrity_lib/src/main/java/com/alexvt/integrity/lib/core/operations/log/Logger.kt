/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.log

import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.core.data.log.LogEntryType
import com.alexvt.integrity.lib.core.data.metadata.Snapshot

abstract class Logger {

    fun v(text: String, snapshot: Snapshot? = null) = logVerbose(text, snapshot)

    fun logVerbose(text: String, snapshot: Snapshot? = null) = LogEntryBuilder(text)
            .type(LogEntryType.VERBOSE)
            .snapshot(snapshot)
            .thread(Thread.currentThread())
            .let { completeBuildAndLog(it) }

    fun log(text: String, snapshot: Snapshot? = null) = LogEntryBuilder(text)
            .snapshot(snapshot)
            .thread(Thread.currentThread())
            .let { completeBuildAndLog(it) }

    fun logError(text: String, throwable: Throwable? = null) = LogEntryBuilder(text)
            .type(LogEntryType.ERROR)
            .throwable(throwable)
            .thread(Thread.currentThread())
            .let { completeBuildAndLog(it) }

    fun logCrash(text: String, throwable: Throwable, thread: Thread) = LogEntryBuilder(text)
            .type(LogEntryType.CRASH)
            .throwable(throwable)
            .thread(thread)
            .let { completeBuildAndLog(it) }


    private fun completeBuildAndLog(logEntryBuilder: LogEntryBuilder) = logEntryBuilder
            .packageName(getPackageName())
            .process(getProcessName())
            .build().let { log(it) }

    private fun log(logEntry: LogEntry) {
        printInConsole(logEntry)
        if (logEntry.type != LogEntryType.VERBOSE) {
            writeToLog(logEntry, recoverFromCrash = (logEntry.type == LogEntryType.CRASH))
        }
    }

    protected abstract fun printInConsole(logEntry: LogEntry)

    protected abstract fun writeToLog(logEntry: LogEntry, recoverFromCrash: Boolean = false)

    abstract fun getPackageName(): String

    abstract fun getProcessName(): String
}