/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import com.alexvt.integrity.core.util.Clearable
import com.alexvt.integrity.lib.log.LogEntry
import io.reactivex.Flowable

/**
 * Manager of repository of app log entries.
 */
interface LogRepository : Clearable {

    /**
     * Adds the entry to the log.
     */
    fun addEntry(logEntry: LogEntry)

    /**
     * Gets log entries ordered by time descending.
     */
    fun getRecentEntries(limit: Int): Flowable<List<LogEntry>>

    /**
     * Gets unread error and crash type log entries ordered by time descending.
     */
    fun getUnreadErrors(limit: Int): Flowable<List<LogEntry>>

    /**
     * Sets all log entries read.
     */
    fun markAllRead()

    /**
     * Deletes all log entries from database
     */
    override fun clear()
}