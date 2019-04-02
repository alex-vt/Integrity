/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.data.search

import android.content.Context
import androidx.room.*
import com.alexvt.integrity.core.data.search.SearchIndexRepository
import com.alexvt.integrity.lib.core.data.search.DataChunk
import com.alexvt.integrity.lib.core.data.search.NamedLink
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Stores text data chunks in SQL DB.
 */
class RoomSearchIndexRepository(context: Context) : SearchIndexRepository {

    @Entity(primaryKeys = ["artifactId", "date", "index"])
    @TypeConverters(PropertyConverters::class)
    data class DbDataChunk(val artifactId: Long,
                           val date: String,
                           val text: String,
                           val index: String, // unique within the snapshot
                           val links: ArrayList<NamedLink>
    )

    class PropertyConverters {
        @TypeConverter
        fun fromString(value: String): ArrayList<NamedLink> {
            return ObjectMapper().readValue(value, object : TypeReference<ArrayList<NamedLink>>(){})
        }

        @TypeConverter
        fun toStringValue(links: ArrayList<NamedLink>): String {
            return ObjectMapper().writeValueAsString(links)
        }
    }

    /**
     * Converts between DB and core data entities.
     */
    private object EntityConverters {
        fun fromDbEntity(value: DbDataChunk) = DataChunk(
                artifactId = value.artifactId,
                date = value.date,
                text = value.text,
                index = value.index,
                links = value.links
        )

        fun toDbEntity(value: DataChunk) = DbDataChunk(
                artifactId = value.artifactId,
                date = value.date,
                text = value.text,
                index = value.index,
                links = value.links
        )
    }

    @Dao
    interface DataChunkDao {
        @Insert
        suspend fun add(dataChunks: List<DbDataChunk>)

        @Query("DELETE FROM dbdatachunk WHERE artifactId = :artifactId")
        suspend fun removeForArtifact(artifactId: Long)

        @Query("DELETE FROM dbdatachunk WHERE artifactId = :artifactId AND date = :date")
        suspend fun removeForSnapshot(artifactId: Long, date: String)

        @Query("SELECT * FROM dbdatachunk WHERE text LIKE :text")
        fun searchText(text: String): Single<List<DbDataChunk>>

        @Query("SELECT * FROM dbdatachunk WHERE artifactId = :artifactId AND text LIKE :text")
        fun searchText(text: String, artifactId: Long): Single<List<DbDataChunk>>

        @Query("DELETE FROM dbdatachunk")
        suspend fun clear()
    }

    @Database(entities = [DbDataChunk::class], version = 1)
    abstract class DataChunkDatabase : RoomDatabase() {
        abstract fun dataChunkDao(): DataChunkDao
    }

    private val db = Room.databaseBuilder(context, DataChunkDatabase::class.java,
            "DataChunkDb").build()

    override fun add(dataChunks: List<DataChunk>) {
        GlobalScope.launch(Dispatchers.Default) {
            dataChunks.map { EntityConverters.toDbEntity(it) }.let { db.dataChunkDao().add(it) }
        }
    }

    override fun removeForArtifact(artifactId: Long) {
        GlobalScope.launch(Dispatchers.Default) {
            db.dataChunkDao().removeForArtifact(artifactId)
        }
    }


    override fun removeForSnapshot(artifactId: Long, date: String) {
        GlobalScope.launch(Dispatchers.Default) {
            db.dataChunkDao().removeForSnapshot(artifactId, date)
        }
    }

    override fun searchTextSingle(text: String): Single<List<DataChunk>> = db.dataChunkDao()
            .searchText("%$text%")
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun searchTextSingle(text: String, artifactId: Long): Single<List<DataChunk>> = db.dataChunkDao()
            .searchText("%$text%", artifactId)
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun clear() {
        GlobalScope.launch(Dispatchers.Default) {
            db.dataChunkDao().clear()
        }
    }
}