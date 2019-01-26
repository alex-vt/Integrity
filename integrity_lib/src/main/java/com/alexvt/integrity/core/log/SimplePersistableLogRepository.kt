/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.content.Context
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Manager of repository of app log entries.
 */
object SimplePersistableLogRepository : LogRepository {

    data class Log(val entries: ArrayList<LogEntry> = arrayListOf()) // Log entry container

    private lateinit var log: Log

    /**
     * Prepares database for use.
     */
    override fun init(context: Context) {
        val logJson = PreferencesUtil.getLogJson(context)
        if (logJson != null) {
            log = JsonSerializerUtil.fromJson(logJson, Log::class.java)
        }
        if (!::log.isInitialized) {
            log = Log()
        }
    }

    private var changesListenerMap: Map<String, (() -> Unit)> = emptyMap()

    /**
     * Registers database contents changes listener with a tag.
     */
    override fun addChangesListener(tag: String, changesListener: () -> Unit) {
        changesListenerMap = changesListenerMap.plus(Pair(tag, changesListener))
        invokeChangesListeners()
    }

    /**
     * Removes database contents changes listener by a tag
     */
    override fun removeChangesListener(tag: String) {
        changesListenerMap = changesListenerMap.minus(tag)
    }

    private fun invokeChangesListeners() {
        GlobalScope.launch (Dispatchers.Main) {
            changesListenerMap.forEach {
                it.value.invoke()
            }
        }
    }

    /**
     * Adds the entry to the log.
     */
    override fun addEntry(logEntry: LogEntry) {
        log.entries.add(logEntry)
        saveChanges()
    }

    /**
     * Gets log entries ordered by addition time descending.
     */
    override fun getRecentEntries(limit: Int): List<LogEntry> {
        return log.entries
                .sortedByDescending { it.orderId }
                .take(limit)
    }

    /**
     * Gets unread error and crash type log entries ordered by addition time descending.
     */
    override fun getUnreadErrors(): List<LogEntry> {
        return log.entries
                .filter { !it.read }
                .filter { it.type == LogEntryType.ERROR || it.type == LogEntryType.CRASH }
                .sortedByDescending { it.orderId }
    }

    /**
     * Sets all log entries read.
     */
    override fun markAllRead() {
        val unreadEntries = log.entries.filter { !it.read }
        log.entries.removeAll(unreadEntries)
        log.entries.addAll(unreadEntries.map { it.copy(read = true) })
        saveChanges()
    }

    /**
     * Deletes all log entries from database
     */
    override fun clear() {
        log.entries.clear()
        saveChanges()
    }

    /**
     * Invokes changes listener and persists log to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges() {
        invokeChangesListeners()
        val logJson = JsonSerializerUtil.toJson(log)
        PreferencesUtil.setLogJson(IntegrityCore.context, logJson)
    }
}