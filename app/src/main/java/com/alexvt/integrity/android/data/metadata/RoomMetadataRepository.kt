/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.data.metadata

import android.content.Context
import androidx.room.*
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.lib.core.data.metadata.DownloadSchedule
import com.alexvt.integrity.lib.core.data.metadata.FolderLocation
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.metadata.Tag
import com.alexvt.integrity.lib.core.data.destinations.LocalFolderLocation
import com.alexvt.integrity.lib.core.data.destinations.SambaFolderLocation
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import io.reactivex.Flowable
import io.reactivex.Single
import kotlin.collections.ArrayList
import java.lang.reflect.ParameterizedType


/**
 * Stores metadata in SQL DB.
 */
class RoomMetadataRepository(context: Context, dbName: String = "SnapshotDb"): MetadataRepository {

    /**
     * Log entry in SQL DB.
     */
    @Entity(primaryKeys = ["artifactId", "date"])
    @TypeConverters(PropertyConverters::class)
    data class DbSnapshot(val artifactId: Long,
                          val date: String,
                          val title: String,
                          val description: String,
                          val downloadSchedule: DownloadSchedule,
                          val archiveFolderLocations: List<FolderLocation>,
                          val tags: List<Tag>,
                          val themeColor: String,
                          val dataTypePackageName: String,
                          val dataTypeClassName: String,
                          val dataTypeSpecificMetadataJson: String,
                          val status: String
    )

    class PropertyConverters {
        private val moshi = Moshi.Builder()
                .add(PolymorphicJsonAdapterFactory.of(FolderLocation::class.java, "FolderLocation")
                        .withSubtype(LocalFolderLocation::class.java, "LocalFolderLocation")
                        .withSubtype(SambaFolderLocation::class.java, "SambaFolderLocation")
                ).build()

        @TypeConverter
        fun downloadScheduleFromString(value: String): DownloadSchedule {
            return moshi.adapter(DownloadSchedule::class.java).fromJson(value)!!
        }

        @TypeConverter
        fun fromDownloadSchedule(value: DownloadSchedule): String {
            return moshi.adapter(DownloadSchedule::class.java).toJson(value)
        }

        @TypeConverter
        fun folderLocationsFromString(value: String): List<FolderLocation> {
            val type: ParameterizedType = Types.newParameterizedType(List::class.java, FolderLocation::class.java)
            return moshi.adapter<List<FolderLocation>>(type).fromJson(value)!!
        }

        @TypeConverter
        fun fromFolderLocations(value: List<FolderLocation>): String {
            val type: ParameterizedType = Types.newParameterizedType(List::class.java, FolderLocation::class.java)
            return moshi.adapter<List<FolderLocation>>(type).toJson(value)
        }

        @TypeConverter
        fun tagsFromString(value: String): List<Tag> {
            val type: ParameterizedType = Types.newParameterizedType(List::class.java, Tag::class.java)
            return moshi.adapter<List<Tag>>(type).fromJson(value)!!
        }

        @TypeConverter
        fun fromTags(value: List<Tag>): String {
            val type: ParameterizedType = Types.newParameterizedType(List::class.java, Tag::class.java)
            return moshi.adapter<List<Tag>>(type).toJson(value)
        }
    }

    /**
     * Converts between DB and core data entities.
     */
    private object EntityConverters {
        fun fromDbEntity(value: DbSnapshot) = Snapshot(
                artifactId = value.artifactId,
                date = value.date,
                title = value.title,
                description = value.description,
                downloadSchedule = value.downloadSchedule,
                archiveFolderLocations = ArrayList(value.archiveFolderLocations),
                tags = ArrayList(value.tags),
                themeColor = value.themeColor,
                dataTypePackageName = value.dataTypePackageName,
                dataTypeClassName = value.dataTypeClassName,
                dataTypeSpecificMetadataJson = value.dataTypeSpecificMetadataJson,
                status = value.status
        )

        fun toDbEntity(value: Snapshot) = DbSnapshot(
                artifactId = value.artifactId,
                date = value.date,
                title = value.title,
                description = value.description,
                downloadSchedule = value.downloadSchedule,
                archiveFolderLocations = value.archiveFolderLocations,
                tags = value.tags,
                themeColor = value.themeColor,
                dataTypePackageName = value.dataTypePackageName,
                dataTypeClassName = value.dataTypeClassName,
                dataTypeSpecificMetadataJson = value.dataTypeSpecificMetadataJson,
                status = value.status
        )
    }

    @Dao
    interface SnapshotDao {
        @Insert
        fun addSnapshotMetadata(snapshot: DbSnapshot)

        @Query("DELETE FROM dbsnapshot WHERE artifactId = :artifactId")
        fun removeArtifactMetadata(artifactId: Long)

        @Query("DELETE FROM dbsnapshot WHERE artifactId = :artifactId AND date = :date")
        fun removeSnapshotMetadata(artifactId: Long, date: String)

        @Query("SELECT * FROM dbsnapshot")
        fun getAllArtifactMetadataFlowable(): Flowable<List<DbSnapshot>>

        @Query("SELECT * FROM dbsnapshot")
        fun getAllArtifactMetadataBlocking(): List<DbSnapshot>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId")
        fun getArtifactMetadataFlowable(artifactId: Long): Flowable<List<DbSnapshot>>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId")
        fun getArtifactMetadataBlocking(artifactId: Long): List<DbSnapshot>

        @Query("SELECT * FROM dbsnapshot GROUP BY artifactId HAVING MAX(date) ORDER BY date")
        fun getAllArtifactLatestMetadataFlowable(): Flowable<List<DbSnapshot>>

        @Query("SELECT * FROM dbsnapshot GROUP BY artifactId HAVING MAX(date) ORDER BY date")
        fun getAllArtifactLatestMetadataBlocking(): List<DbSnapshot>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId ORDER BY date DESC LIMIT 1")
        fun getLatestSnapshotMetadataFlowable(artifactId: Long): Flowable<List<DbSnapshot>>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId ORDER BY date DESC LIMIT 1")
        fun getLatestSnapshotMetadataBlocking(artifactId: Long): List<DbSnapshot>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId AND date = :date")
        fun getSnapshotMetadataFlowable(artifactId: Long, date: String): Flowable<List<DbSnapshot>>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId AND date = :date")
        fun getSnapshotMetadataBlocking(artifactId: Long, date: String): List<DbSnapshot>

        @Query("SELECT * FROM dbsnapshot WHERE title LIKE :text")
        fun searchTitleSingle(text: String): Single<List<DbSnapshot>>

        @Query("SELECT * FROM dbsnapshot WHERE artifactId = :artifactId AND title LIKE :text")
        fun searchTitleSingle(text: String, artifactId: Long): Single<List<DbSnapshot>>

        @Query("DELETE FROM dbsnapshot")
        fun clear()

        @Query("DELETE FROM dbsnapshot WHERE artifactId = :artifactId AND status = 'blueprint'")
        fun cleanupArtifactBlueprints(artifactId: Long)
    }

    @Database(entities = [DbSnapshot::class], version = 1)
    abstract class SnapshotDatabase : RoomDatabase() {
        abstract fun snapshotDao(): SnapshotDao
    }

    private val db = Room.databaseBuilder(context, SnapshotDatabase::class.java,
            dbName).build()


    override fun addSnapshotMetadata(snapshot: Snapshot) {
        EntityConverters.toDbEntity(snapshot).let { db.snapshotDao().addSnapshotMetadata(it) }
    }

    override fun removeArtifactMetadata(artifactId: Long) {
        db.snapshotDao().removeArtifactMetadata(artifactId)
    }

    override fun removeSnapshotMetadata(artifactId: Long, date: String) {
        db.snapshotDao().removeSnapshotMetadata(artifactId, date)
    }

    override fun getAllArtifactMetadataBlocking(): List<Snapshot> = db.snapshotDao()
            .getAllArtifactMetadataBlocking()
            .map { EntityConverters.fromDbEntity(it) }

    override fun getAllArtifactMetadataFlowable(): Flowable<List<Snapshot>> = db.snapshotDao()
            .getAllArtifactMetadataFlowable()
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun getAllArtifactLatestMetadataBlocking(): List<Snapshot> = db.snapshotDao()
            .getAllArtifactLatestMetadataBlocking()
            .map { EntityConverters.fromDbEntity(it) }

    override fun getAllArtifactLatestMetadataFlowable(): Flowable<List<Snapshot>> = db.snapshotDao()
            .getAllArtifactLatestMetadataFlowable()
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun getArtifactMetadataBlocking(artifactId: Long): List<Snapshot> = db.snapshotDao()
            .getArtifactMetadataBlocking(artifactId)
            .map { EntityConverters.fromDbEntity(it) }

    override fun getArtifactMetadataFlowable(artifactId: Long): Flowable<List<Snapshot>> = db.snapshotDao()
            .getArtifactMetadataFlowable(artifactId)
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun getLatestSnapshotMetadataBlocking(artifactId: Long): Snapshot = db.snapshotDao()
            .getLatestSnapshotMetadataBlocking(artifactId)
            .map { EntityConverters.fromDbEntity(it) }
            .first()

    override fun getLatestSnapshotMetadataFlowable(artifactId: Long): Flowable<Snapshot> = db.snapshotDao()
            .getLatestSnapshotMetadataFlowable(artifactId)
            .map { it.first() }
            .map { EntityConverters.fromDbEntity(it) }

    override fun getSnapshotMetadataBlocking(artifactId: Long, date: String): Snapshot = db.snapshotDao()
            .getSnapshotMetadataBlocking(artifactId, date)
            .map { EntityConverters.fromDbEntity(it) }
            .first()

    override fun getSnapshotMetadataFlowable(artifactId: Long, date: String): Flowable<Snapshot> = db.snapshotDao()
            .getSnapshotMetadataFlowable(artifactId, date)
            .map { it.first() }
            .map { EntityConverters.fromDbEntity(it) }

    override fun searchTitleSingle(searchText: String): Single<List<Snapshot>> = db.snapshotDao()
            .searchTitleSingle("%$searchText%")
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun searchTitleSingle(searchText: String, artifactId: Long): Single<List<Snapshot>> = db.snapshotDao()
            .searchTitleSingle("%$searchText%", artifactId)
            .map { it.map { EntityConverters.fromDbEntity(it) } }

    override fun clear() {
        db.snapshotDao().clear()
    }

    override fun cleanupArtifactBlueprints(artifactId: Long) {
        db.snapshotDao().cleanupArtifactBlueprints(artifactId)
    }

}