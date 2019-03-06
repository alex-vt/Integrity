/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.database

import android.content.Context
import com.alexvt.integrity.core.util.HashUtil
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.lib.MetadataCollection
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotCompareUtil
import com.alexvt.integrity.lib.SnapshotStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Stores metadata simply in Java objects and persists them to Android SharedPreferences
 * as JSON string.
 */
object SimplePersistableMetadataRepository: MetadataRepository {

    private lateinit var allMetadata: MetadataCollection

    /**
     * Reads metadata from JSON string to Java objects
     */
    override fun init(context: Context, clear: Boolean) {
        if (!clear) {
            val fullMetadataJson = readJsonFromStorage(context)
            if (fullMetadataJson != null) {
                allMetadata = JsonSerializerUtil.fromJson(fullMetadataJson, MetadataCollection::class.java)
            }
        }
        if (clear || !::allMetadata.isInitialized) {
            val snapshotMetadataList = arrayListOf<Snapshot>()
            allMetadata = MetadataCollection(snapshotMetadataList,
                    HashUtil.getHash(snapshotMetadataList))
            saveChanges(context)
        }
    }

    private var changesListenerMap: Map<String, (() -> Unit)> = emptyMap()

    /**
     * Registers database contents changes listener with a tag.
     */
    override fun addChangesListener(context: Context, changesListener: () -> Unit) {
        changesListenerMap = changesListenerMap.plus(Pair(context.toString(), changesListener))
        invokeChangesListeners()
    }

    /**
     * Removes database contents changes listener by a tag
     */
    override fun removeChangesListener(context: Context) {
        changesListenerMap = changesListenerMap.minus(context.toString())
    }

    private fun invokeChangesListeners() {
        GlobalScope.launch (Dispatchers.Main) {
            changesListenerMap.forEach {
                it.value.invoke()
            }
        }
    }

    override fun addSnapshotMetadata(context: Context, snapshot: Snapshot) {
        allMetadata.snapshots.add(snapshot)
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun removeArtifactMetadata(context: Context, artifactId: Long) {
        allMetadata.snapshots.removeIf { it.artifactId == artifactId }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun removeSnapshotMetadata(context: Context, artifactId: Long, date: String) {
        allMetadata.snapshots.removeIf { it.artifactId == artifactId && it.date == date }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun getAllArtifactMetadata(): MetadataCollection {
        return allMetadata
    }

    override fun getAllArtifactLatestMetadata(deprioritizeBlueprints: Boolean): MetadataCollection {
        val snapshotComparator = if (deprioritizeBlueprints) {
            SnapshotCompareUtil.blueprintLowPriorityComparator.thenByDescending { it.date }
        } else {
            compareByDescending { it.date }
        }
        val artifactSnapshotMetadataList = allMetadata.snapshots
                .groupBy { it.artifactId }
                .map { it.value
                        .sortedWith(snapshotComparator)
                        .first() }
                .sortedByDescending { it.date }
        return MetadataCollection(ArrayList(artifactSnapshotMetadataList),
                HashUtil.getHash(artifactSnapshotMetadataList))
    }

    override fun getArtifactMetadata(artifactId: Long): MetadataCollection {
        val artifactSnapshotMetadataList = allMetadata.snapshots
                .filter { it.artifactId == artifactId }
                .sortedByDescending { it.date }
        return MetadataCollection(ArrayList(artifactSnapshotMetadataList),
                HashUtil.getHash(artifactSnapshotMetadataList))
    }

    override fun getLatestSnapshotMetadata(artifactId: Long): Snapshot {
        return allMetadata.snapshots
                .filter { it.artifactId == artifactId }
                .sortedByDescending { it.date }
                .first()
    }

    override fun getSnapshotMetadata(artifactId: Long, date: String): Snapshot {
        return allMetadata.snapshots
                .first { it.artifactId == artifactId && it.date == date }
    }

    override fun clear(context: Context) {
        allMetadata.snapshots.clear()
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges(context)
    }

    override fun cleanupArtifactBlueprints(context: Context, artifactId: Long) {
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
        invokeChangesListeners()
        val fullMetadataJson = JsonSerializerUtil.toJson(allMetadata)
        persistJsonToStorage(context, fullMetadataJson)
    }


    // Storage for the JSON string in SharedPreferences

    private const val TAG = "snapshots_metadata"

    private const val preferencesName = "persisted_$TAG"
    private const val preferenceKey = "${TAG}_json"

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}