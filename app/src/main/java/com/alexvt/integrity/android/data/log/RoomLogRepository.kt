/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.data.log

import android.content.Context
import androidx.room.*
import com.alexvt.integrity.core.data.log.LogRepository
import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.core.data.log.LogEntryType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Manager of repository of app build entries, using Room.
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
        fun getRecentEntriesFlowable(limit: Int): Flowable<List<DbLogEntry>>

        @Query("SELECT * FROM dblogentry ORDER BY orderId DESC LIMIT :limit")
        fun getRecentEntriesBlocking(limit: Int): List<DbLogEntry>

        @Query("SELECT * FROM dblogentry WHERE read = 0 " +
                "AND type IN (\'${LogEntryType.ERROR}\', \'${LogEntryType.CRASH}\') " +
                "ORDER BY orderId DESC LIMIT :limit")
        fun getUnreadErrorsFlowable(limit: Int): Flowable<List<DbLogEntry>>

        @Query("SELECT * FROM dblogentry WHERE read = 0 " +
                "AND type IN (\'${LogEntryType.ERROR}\', \'${LogEntryType.CRASH}\') " +
                "ORDER BY orderId DESC LIMIT :limit")
        fun getUnreadErrorsBlocking(limit: Int): List<DbLogEntry>

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

    /**
     * Adds the entry to the build.
     */
    override fun addEntry(logEntry: LogEntry) {
        GlobalScope.launch(Dispatchers.Default) {
            EntityConverters.toDbEntity(logEntry).let { db.logEntryDao().addEntry(it) }
        }
    }

    /**
     * Gets build entries ordered by addition time descending.
     */
    override fun getRecentEntriesFlowable(limit: Int): Flowable<List<LogEntry>> = db.logEntryDao()
            .getRecentEntriesFlowable(limit)
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun getRecentEntriesBlocking(limit: Int): List<LogEntry> = db.logEntryDao()
            .getRecentEntriesBlocking(limit)
            .map { EntityConverters.fromDbEntity(it) }

    /**
     * Gets unread error and crash type build entries ordered by addition time descending.
     */
    override fun getUnreadErrorsFlowable(limit: Int): Flowable<List<LogEntry>> = db.logEntryDao()
            .getUnreadErrorsFlowable(limit)
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun getUnreadErrorsBlocking(limit: Int): List<LogEntry> = db.logEntryDao()
            .getUnreadErrorsBlocking(limit)
            .map { EntityConverters.fromDbEntity(it) }

    /**
     * Sets all build entries read.
     */
    override fun markAllRead() {
        GlobalScope.launch(Dispatchers.Default) {
            db.logEntryDao().markAllRead()
        }
    }

    /**
     * Deletes all build entries from database
     */
    override fun clear() {
        GlobalScope.launch(Dispatchers.Default) {
            db.logEntryDao().clear()
        }
    }
}