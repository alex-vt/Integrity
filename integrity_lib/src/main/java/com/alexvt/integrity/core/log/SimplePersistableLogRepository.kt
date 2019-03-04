/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.content.Context
import com.alexvt.integrity.core.util.JsonSerializerUtil
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
    override fun init(context: Context, clear: Boolean) {
        if (!clear) {
            val logJson = readJsonFromStorage(context)
            if (logJson != null) {
                log = JsonSerializerUtil.fromJson(logJson, Log::class.java)
            }
        }
        if (clear || !::log.isInitialized) {
            log = Log()
            saveChanges(context)
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
    override fun addEntry(context: Context, logEntry: LogEntry) {
        log.entries.add(logEntry)
        saveChanges(context)
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
    override fun markAllRead(context: Context) {
        val unreadEntries = log.entries.filter { !it.read }
        log.entries.removeAll(unreadEntries)
        log.entries.addAll(unreadEntries.map { it.copy(read = true) })
        saveChanges(context)
    }

    /**
     * Deletes all log entries from database
     */
    override fun clear(context: Context) {
        log.entries.clear()
        saveChanges(context)
    }

    /**
     * Invokes changes listener and persists log to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges(context: Context) {
        invokeChangesListeners()
        val logJson = JsonSerializerUtil.toJson(log)
        persistJsonToStorage(context, logJson)
    }


    // Storage for the JSON string in SharedPreferences

    private const val TAG = "log"

    private const val preferencesName = "persisted_$TAG"
    private const val preferenceKey = "${TAG}_json"

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}