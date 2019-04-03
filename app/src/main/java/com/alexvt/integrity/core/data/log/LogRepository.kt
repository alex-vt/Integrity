/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.data.log

import com.alexvt.integrity.core.data.Clearable
import com.alexvt.integrity.lib.core.data.log.LogEntry
import io.reactivex.Flowable

/**
 * Manager of repository of app build entries.
 */
interface LogRepository : Clearable {

    /**
     * Adds the entry to the build.
     */
    fun addEntry(logEntry: LogEntry)

    /**
     * Gets build entries ordered by time descending.
     */
    fun getRecentEntriesBlocking(limit: Int): List<LogEntry>

    fun getRecentEntriesFlowable(limit: Int): Flowable<List<LogEntry>>

    /**
     * Gets unread error and crash type build entries ordered by time descending.
     */
    fun getUnreadErrorsBlocking(limit: Int): List<LogEntry>

    fun getUnreadErrorsFlowable(limit: Int): Flowable<List<LogEntry>>

    /**
     * Sets all build entries read.
     */
    fun markAllRead()

    /**
     * Deletes all build entries from database
     */
    override fun clear()
}