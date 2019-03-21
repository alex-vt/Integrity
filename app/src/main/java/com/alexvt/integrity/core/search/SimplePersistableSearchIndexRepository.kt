/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

import android.content.Context
import com.alexvt.integrity.lib.search.DataChunk
import com.alexvt.integrity.lib.search.DataChunks
import com.alexvt.integrity.lib.util.JsonSerializerUtil
import io.reactivex.Single

/**
 * Stores text data chunks simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 */
class SimplePersistableSearchIndexRepository(private val context: Context) : SearchIndexRepository {

    private var allDataChunks: DataChunks

    // Storage name for the JSON string in SharedPreferences
    private val TAG = "search_index"
    private val preferencesName = "persisted_$TAG"
    private val preferenceKey = "${TAG}_json"

    init {
        val dataChunksJson = readJsonFromStorage(context)
        if (dataChunksJson != null) {
            allDataChunks = JsonSerializerUtil.fromJson(dataChunksJson, DataChunks::class.java)
        } else {
            allDataChunks = DataChunks()
            persistAll(context)
        }
    }

    override fun add(dataChunks: List<DataChunk>) {
        allDataChunks.chunks.addAll(dataChunks)
        persistAll(context)
    }

    override fun removeForArtifact(artifactId: Long) {
        allDataChunks.chunks.removeIf { it.artifactId == artifactId }
        persistAll(context)
    }

    override fun removeForSnapshot(artifactId: Long, date: String) {
        allDataChunks.chunks.removeIf { it.artifactId == artifactId && it.date == date }
        persistAll(context)
    }

    override fun searchText(text: String): Single<List<DataChunk>> = Single.just(
            allDataChunks.chunks
                    .filter { it.text.contains(text) }
    )

    override fun searchText(text: String, artifactId: Long): Single<List<DataChunk>> = Single.just(
            allDataChunks.chunks
                    .filter { it.artifactId == artifactId && it.text.contains(text) }
    )

    override fun clear() {
        allDataChunks.chunks.clear()
        persistAll(context)
    }

    /**
     * Persists tags to JSON in SharedPreferences.
     *
     * Should be called after every tag modification.
     */
    @Synchronized private fun persistAll(context: Context) {
        val dataChunksJson = JsonSerializerUtil.toJson(allDataChunks)
        persistJsonToStorage(context, dataChunksJson)
    }


    // Storage for the JSON string in SharedPreferences

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}