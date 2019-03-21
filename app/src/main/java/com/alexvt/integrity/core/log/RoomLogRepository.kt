/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import android.content.Context
import androidx.room.*
import com.alexvt.integrity.lib.log.LogEntry
import com.alexvt.integrity.lib.log.LogEntryType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Manager of repository of app log entries, using Room.
 */
class RoomLogRepository(context: Context) : LogRepository {

    /**
     * Log entry in SQL DB.
     */
    @Entity
    @TypeConverters(PropertyConverters::class)
    data class DbLogEntry(@PrimaryKey val orderId: String,
                          val time: String,
                          val tag: String,
                          val text: String,
                          val data: LinkedHashMap<String, String>,
                          val stackTraceText: String,
                          val type: String,
                          val read: Boolean
    )

    class PropertyConverters {
        @TypeConverter
        fun fromString(value: String): LinkedHashMap<String, String> {
            return ObjectMapper().readValue(value, object : TypeReference<LinkedHashMap<String, String>>(){})
        }

        @TypeConverter
        fun fromStringMap(map: LinkedHashMap<String, String>): String {
            return ObjectMapper().writeValueAsString(map)
        }
    }

    /**
     * Converts between DB and core data entities.
     */
    private object EntityConverters {
        fun fromDbEntity(value: DbLogEntry) = LogEntry(
                orderId = value.orderId,
                time = value.time,
                tag = value.tag,
                text = value.text,
                data = value.data,
                stackTraceText = value.stackTraceText,
                type = value.type,
                read = value.read
        )

        fun toDbEntity(value: LogEntry) = DbLogEntry(
                orderId = value.orderId,
                time = value.time,
                tag = value.tag,
                text = value.text,
                data = value.data,
                stackTraceText = value.stackTraceText,
                type = value.type,
                read = value.read
        )
    }

    @Dao
    interface LogEntryDao {
        @Insert
        suspend fun addEntry(logEntry: DbLogEntry)

        @Query("SELECT * FROM dblogentry ORDER BY orderId DESC LIMIT :limit")
        suspend fun getRecentEntries(limit: Int): List<DbLogEntry>

        @Query("SELECT * FROM dblogentry WHERE read = 0 AND type IN (\'${LogEntryType.ERROR}\', \'${LogEntryType.CRASH}\') ORDER BY orderId DESC ")
        suspend fun getUnreadErrors(): List<DbLogEntry>

        @Query("UPDATE dblogentry SET read = 1 WHERE read = 0")
        suspend fun markAllRead()

        @Query("DELETE FROM dblogentry")
        suspend fun clear()
    }
    
    @Database(entities = [DbLogEntry::class], version = 1)
    abstract class LogEntryDatabase : RoomDatabase() {
        abstract fun logEntryDao(): LogEntryDao
    }

    private val db = Room.databaseBuilder(context, LogEntryDatabase::class.java, 
            "LogEntryDb").build()

    
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
        changesListenerMap.forEach {
            it.value.invoke()
        }
    }

    /**
     * Adds the entry to the log.
     */
    override fun addEntry(logEntry: LogEntry) {
        runBlocking(Dispatchers.Default) {
            EntityConverters.toDbEntity(logEntry).let { db.logEntryDao().addEntry(it) }
        }
        invokeChangesListeners()
    }

    /**
     * Gets log entries ordered by addition time descending.
     */
    override fun getRecentEntries(limit: Int, resultListener: (List<LogEntry>) -> Unit) {
        resultListener.invoke(runBlocking(Dispatchers.Default) {
            db.logEntryDao().getRecentEntries(limit).map { EntityConverters.fromDbEntity(it) }
        })
    }

    /**
     * Gets unread error and crash type log entries ordered by addition time descending.
     */
    override fun getUnreadErrors(resultListener: (List<LogEntry>) -> Unit) {
        resultListener.invoke(runBlocking(Dispatchers.Default) {
            db.logEntryDao().getUnreadErrors().map { EntityConverters.fromDbEntity(it) }
        })
    }

    /**
     * Sets all log entries read.
     */
    override fun markAllRead() {
        runBlocking(Dispatchers.Default) {
            db.logEntryDao().markAllRead()
        }
        invokeChangesListeners()
    }

    /**
     * Deletes all log entries from database
     */
    override fun clear() {
        runBlocking(Dispatchers.Default) {
            db.logEntryDao().clear()
        }
        invokeChangesListeners()
    }
}