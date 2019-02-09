/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

import android.content.Context
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil

/**
 * Stores text data chunks simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 */
object SimplePersistableSearchIndexRepository : SearchIndexRepository {

    private lateinit var allDataChunks: DataChunks


    /**
     * Prepares database for use
     */
    override fun init(context: Context) {
        val dataChunksJson = PreferencesUtil.getDataChunksJson(context)
        if (dataChunksJson != null) {
            allDataChunks = JsonSerializerUtil.fromJson(dataChunksJson, DataChunks::class.java)
        }
        if (!::allDataChunks.isInitialized) {
            allDataChunks = DataChunks()
        }
    }

    override fun add(dataChunks: List<DataChunk>) {
        allDataChunks.chunks.addAll(dataChunks)
        persistAll()
    }

    override fun removeForArtifact(artifactId: Long) {
        allDataChunks.chunks.removeIf { it.artifactId == artifactId }
        persistAll()
    }

    override fun removeForSnapshot(artifactId: Long, date: String) {
        allDataChunks.chunks.removeIf { it.artifactId == artifactId && it.date == date }
        persistAll()
    }

    override fun searchText(text: String) = allDataChunks.chunks
            .filter { it.text.contains(text) }

    override fun searchText(text: String, artifactId: Long) = allDataChunks.chunks
            .filter { it.artifactId == artifactId && it.text.contains(text) }

    override fun clear() {
        allDataChunks.chunks.clear()
        persistAll()
    }

    /**
     * Persists tags to JSON in SharedPreferences.
     *
     * Should be called after every tag modification.
     */
    @Synchronized private fun persistAll() {
        val dataChunksJson = JsonSerializerUtil.toJson(allDataChunks)
        PreferencesUtil.setDataChunksJson(IntegrityCore.context, dataChunksJson)
    }
}