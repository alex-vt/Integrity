/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.content.Context
import com.alexvt.integrity.core.util.ReactiveRequestPool
import com.alexvt.integrity.lib.log.LogEntry
import com.alexvt.integrity.lib.log.LogEntryType
import com.alexvt.integrity.lib.util.JsonSerializerUtil

/**
 * Manager of repository of app log entries.
 */
class SimplePersistableLogRepository(
        private val context: Context,
        private val reactiveRequests: ReactiveRequestPool = ReactiveRequestPool()
) : LogRepository {

    private data class Log(val entries: ArrayList<LogEntry> = arrayListOf()) // Log entry container

    private var log: Log

    // Storage name for the JSON string in SharedPreferences
    private val TAG = "log"
    private val preferencesName = "persisted_$TAG"
    private val preferenceKey = "${TAG}_json"

    init {
        val logJson = readJsonFromStorage(context)
        if (logJson != null) {
            log = JsonSerializerUtil.fromJson(logJson, Log::class.java)
        } else {
            log = Log()
            saveChanges(context)
        }
    }

    override fun getRecentEntriesBlocking(limit: Int)
            = log.entries.sortedByDescending { it.orderId }
            .take(limit)

    override fun getRecentEntriesFlowable(limit: Int) = reactiveRequests.add {
        getRecentEntriesBlocking(limit)
    }

    override fun getUnreadErrorsBlocking(limit: Int): List<LogEntry>
            = log.entries.filter { !it.read }
            .filter { it.type == LogEntryType.ERROR || it.type == LogEntryType.CRASH }
            .sortedByDescending { it.orderId }
            .take(limit)

    override fun getUnreadErrorsFlowable(limit: Int) = reactiveRequests.add {
        getUnreadErrorsBlocking(limit)
    }

    /**
     * Adds the entry to the log.
     */
    override fun addEntry(logEntry: LogEntry) {
        log.entries.add(logEntry)
        saveChanges(context)
    }

    /**
     * Sets all log entries read.
     */
    override fun markAllRead() {
        val unreadEntries = log.entries.filter { !it.read }
        log.entries.removeAll(unreadEntries)
        log.entries.addAll(unreadEntries.map { it.copy(read = true) })
        saveChanges(context)
    }

    /**
     * Deletes all log entries from database
     */
    override fun clear() {
        log.entries.clear()
        saveChanges(context)
    }

    /**
     * Invokes changes listener and persists log to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges(context: Context) {
        reactiveRequests.emitCurrentDataAll()
        val logJson = JsonSerializerUtil.toJson(log)
        persistJsonToStorage(context, logJson)
    }


    // Storage for the JSON string in SharedPreferences

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}