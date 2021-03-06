/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.database

import android.content.Context
import com.alexvt.integrity.core.MetadataCollection
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.util.HashUtil
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil

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

    override fun addSnapshotMetadata(snapshotMetadata: SnapshotMetadata) {
        allMetadata.snapshotMetadataList.add(snapshotMetadata)
        allMetadata = HashUtil.updateHash(allMetadata)
        persistAll()
    }

    override fun removeArtifactMetadata(artifactId: Long) {
        val snapshotMetadataCollectionToRemove = getArtifactMetadata(artifactId)
        allMetadata.snapshotMetadataList.removeAll(snapshotMetadataCollectionToRemove.snapshotMetadataList)
        allMetadata = HashUtil.updateHash(allMetadata)
        persistAll()
    }

    override fun removeSnapshotMetadata(artifactId: Long, date: String) {
        val snapshotMetadataToRemove = getSnapshotMetadata(artifactId, date)
        allMetadata.snapshotMetadataList.remove(snapshotMetadataToRemove)
        allMetadata = HashUtil.updateHash(allMetadata)
        persistAll()
    }

    override fun getAllArtifactMetadata(): MetadataCollection {
        return allMetadata
    }

    override fun getAllArtifactLatestMetadata(): MetadataCollection {
        val artifactSnapshotMetadataList = allMetadata.snapshotMetadataList
                .groupBy { it.artifactId }
                .map { it.value
                        .sortedByDescending { it.date }
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
        persistAll()
    }

    /**
     * Persists metadata to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun persistAll() {
        val fullMetadataJson = JsonSerializerUtil.toJson(allMetadata)!!
        PreferencesUtil.setFullMetadataJson(IntegrityCore.context, fullMetadataJson)
    }

}