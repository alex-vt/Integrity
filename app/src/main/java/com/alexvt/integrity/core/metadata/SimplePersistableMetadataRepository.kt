/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.metadata

import android.content.Context
import com.alexvt.integrity.core.operations.HashUtil
import com.alexvt.integrity.core.util.ReactiveRequestPool
import com.alexvt.integrity.lib.metadata.MetadataCollection
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotCompareUtil
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.lib.util.JsonSerializerUtil
import io.reactivex.Flowable

/**
 * Stores metadata simply in Java objects and persists them to Android SharedPreferences
 * as JSON string.
 */
class SimplePersistableMetadataRepository(
        private val context: Context,
        private val reactiveRequests: ReactiveRequestPool = ReactiveRequestPool()
): MetadataRepository {

    private var allMetadata: MetadataCollection

    // Storage name for the JSON string in SharedPreferences
    private val TAG = "snapshots_metadata"
    private val preferencesName = "persisted_$TAG"
    private val preferenceKey = "${TAG}_json"

    init {
        val fullMetadataJson = readJsonFromStorage(context)
        if (fullMetadataJson != null) {
            allMetadata = JsonSerializerUtil.fromJson(fullMetadataJson, MetadataCollection::class.java)
        } else {
            val snapshotMetadataList = arrayListOf<Snapshot>()
            allMetadata = MetadataCollection(snapshotMetadataList,
                    HashUtil.getHash(snapshotMetadataList)) // todo move hash calculations out
            saveChanges(context)
        }
    }


    override fun addSnapshotMetadata(snapshot: Snapshot) {
        allMetadata.snapshots.add(snapshot)
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun removeArtifactMetadata(artifactId: Long) {
        allMetadata.snapshots.removeIf { it.artifactId == artifactId }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun removeSnapshotMetadata(artifactId: Long, date: String) {
        allMetadata.snapshots.removeIf { it.artifactId == artifactId && it.date == date }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun getAllArtifactMetadataBlocking(): List<Snapshot>
            = allMetadata.snapshots

    override fun getAllArtifactMetadataFlowable(): Flowable<List<Snapshot>> = reactiveRequests.add {
        getAllArtifactMetadataBlocking()
    }

    override fun getAllArtifactLatestMetadataBlocking(): List<Snapshot> {
        return allMetadata.snapshots
                .groupBy { it.artifactId }
                .map { it.value
                        .sortedWith(compareByDescending { it.date })
                        .first() }
                .sortedByDescending { it.date }
    }

    override fun getAllArtifactLatestMetadataFlowable(): Flowable<List<Snapshot>> = reactiveRequests.add {
        getAllArtifactLatestMetadataBlocking()
    }

    override fun getArtifactMetadataBlocking(artifactId: Long)
            = allMetadata.snapshots
            .filter { it.artifactId == artifactId }
            .sortedByDescending { it.date }

    override fun getArtifactMetadataFlowable(artifactId: Long
    ): Flowable<List<Snapshot>> = reactiveRequests.add {
        getArtifactMetadataBlocking(artifactId)
    }

    override fun getLatestSnapshotMetadataBlocking(artifactId: Long): Snapshot {
        return allMetadata.snapshots
                .filter { it.artifactId == artifactId }
                .sortedByDescending { it.date }
                .first()
    }

    override fun getLatestSnapshotMetadataFlowable(artifactId: Long
    ): Flowable<Snapshot> = reactiveRequests.add {
        getLatestSnapshotMetadataBlocking(artifactId)
    }

    override fun getSnapshotMetadataBlocking(artifactId: Long, date: String): Snapshot {
        return allMetadata.snapshots
                .first { it.artifactId == artifactId && it.date == date }
    }

    override fun getSnapshotMetadataFlowable(artifactId: Long, date: String
    ): Flowable<Snapshot> = reactiveRequests.add {
        getSnapshotMetadataBlocking(artifactId, date)
    }

    override fun clear() {
        allMetadata.snapshots.clear()
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun cleanupArtifactBlueprints(artifactId: Long) {
        allMetadata.snapshots.removeIf { it.artifactId == artifactId
                && it.status == SnapshotStatus.BLUEPRINT }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    /**
     * Invokes changes listener and persists metadata to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges(context: Context) {
        reactiveRequests.emitCurrentDataAll()
        val fullMetadataJson = JsonSerializerUtil.toJson(allMetadata)
        persistJsonToStorage(context, fullMetadataJson)
    }


    // Storage for the JSON string in SharedPreferences

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}