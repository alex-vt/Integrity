/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.database

import android.content.Context
import com.alexvt.integrity.core.*
import com.alexvt.integrity.core.util.HashUtil
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil
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
    override fun init(context: Context) {
        val fullMetadataJson = PreferencesUtil.getFullMetadataJson(IntegrityCore.context)
        val metadataFromJson = JsonSerializerUtil.fromJson(fullMetadataJson, MetadataCollection::class.java)
        if (metadataFromJson != null) {
            allMetadata = metadataFromJson
        } else {
            val snapshotMetadataList = arrayListOf<SnapshotMetadata>()
            allMetadata = MetadataCollection(snapshotMetadataList,
                    HashUtil.getHash(snapshotMetadataList))
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

    override fun addSnapshotMetadata(snapshotMetadata: SnapshotMetadata) {
        allMetadata.snapshotMetadataList.add(snapshotMetadata)
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges()
    }

    override fun removeArtifactMetadata(artifactId: Long) {
        allMetadata.snapshotMetadataList.removeIf { it.artifactId == artifactId }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges()
    }

    override fun removeSnapshotMetadata(artifactId: Long, date: String) {
        allMetadata.snapshotMetadataList.removeIf { it.artifactId == artifactId && it.date == date }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges()
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
        val artifactSnapshotMetadataList = allMetadata.snapshotMetadataList
                .groupBy { it.artifactId }
                .map { it.value
                        .sortedWith(snapshotComparator)
                        .first() }
                .sortedByDescending { it.date }
        return MetadataCollection(ArrayList(artifactSnapshotMetadataList),
                HashUtil.getHash(artifactSnapshotMetadataList))
    }

    override fun getArtifactMetadata(artifactId: Long): MetadataCollection {
        val artifactSnapshotMetadataList = allMetadata.snapshotMetadataList
                .filter { it.artifactId == artifactId }
                .sortedByDescending { it.date }
        return MetadataCollection(ArrayList(artifactSnapshotMetadataList),
                HashUtil.getHash(artifactSnapshotMetadataList))
    }

    override fun getLatestSnapshotMetadata(artifactId: Long): SnapshotMetadata {
        return allMetadata.snapshotMetadataList
                .filter { it.artifactId == artifactId }
                .sortedByDescending { it.date }
                .first()
    }

    override fun getSnapshotMetadata(artifactId: Long, date: String): SnapshotMetadata {
        return allMetadata.snapshotMetadataList
                .first { it.artifactId == artifactId && it.date == date }
    }

    override fun clear() {
        allMetadata.snapshotMetadataList.clear()
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges()
    }

    override fun cleanupArtifactBlueprints(artifactId: Long) {
        allMetadata.snapshotMetadataList.removeIf { it.artifactId == artifactId
                && it.status == SnapshotStatus.BLUEPRINT }
        allMetadata = HashUtil.updateHash(allMetadata)
        saveChanges()
    }

    /**
     * Invokes changes listener and persists metadata to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges() {
        invokeChangesListeners()
        val fullMetadataJson = JsonSerializerUtil.toJson(allMetadata)!!
        PreferencesUtil.setFullMetadataJson(IntegrityCore.context, fullMetadataJson)
    }

}