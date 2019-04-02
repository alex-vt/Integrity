/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.log

import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.core.data.log.LogEntryType

abstract class LogManager {

    fun log(logEntry: LogEntry) {
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