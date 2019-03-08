/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.search

import com.alexvt.integrity.lib.util.JsonSerializerUtil
import com.alexvt.integrity.lib.filesystem.DataFolderManager

class SearchIndexManager(private val dataFolderManager: DataFolderManager) {

    fun addDataForSearchIndex(dataFolderName: String, artifactId: Long,
                              date: String, text: String, index: String,
                              vararg links: Pair<String, String>) {
        val dataChunk = DataChunk(artifactId, date, text, index,
                ArrayList(links.map { NamedLink(it.first, it.second) }))
        val dataChunkJson = JsonSerializerUtil.toJson(dataChunk)

        val textSearchIndexPath = dataFolderManager.getSnapshotDataChunksPath(dataFolderName, artifactId,
                date)

        // todo prevent duplicates
        val prefix = if (dataFolderManager.fileExists(textSearchIndexPath)) ",\n" else ""
        dataFolderManager.addTextToFile("$prefix$dataChunkJson", textSearchIndexPath)
    }

}