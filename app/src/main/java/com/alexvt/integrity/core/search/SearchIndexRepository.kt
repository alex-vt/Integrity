/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

import com.alexvt.integrity.core.util.Clearable
import com.alexvt.integrity.lib.search.DataChunk

/**
 * Manager of repository of text data chunks for search in.
 *
 * A crude unified search index for heterogeneous data. // todo replace with efficient index
 */
interface SearchIndexRepository : Clearable {

    /**
     * Adds data chunks
     */
    fun add(dataChunks: List<DataChunk>)

    /**
     * Removes data chunks for artifact
     */
    fun removeForArtifact(artifactId: Long)

    /**
     * Removes data chunks for snapshot
     */
    fun removeForSnapshot(artifactId: Long, date: String)

    /**
     * Gets all data chunks text of which which contain the given text
     */
    fun searchText(text: String): List<DataChunk>

    /**
     * Gets all data chunks with the given artifact ID and text of which contains the given text
     */
    fun searchText(text: String, artifactId: Long): List<DataChunk>

    /**
     * Deletes all tags from database
     */
    override fun clear()
}