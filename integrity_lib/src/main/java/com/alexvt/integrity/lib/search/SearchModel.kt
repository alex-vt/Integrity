/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.search

import com.alexvt.integrity.lib.metadata.Snapshot

data class DataChunk(val artifactId: Long = 0,
                     val date: String = "",
                     val text: String = "",
                     val index: String = "", // unique within the snapshot
                     val links: ArrayList<NamedLink> = arrayListOf()
)

data class DataChunks(val chunks: ArrayList<DataChunk> = arrayListOf())

data class NamedLink(val title: String = "", val link: String = "")

/**
 * User viewable data chunk part containing its text truncated around the searched text in it,
 * and the most relevant links (around the searched text, and general ones in the data chunk).
 */
data class TextSearchResult(val snapshotTitle: String,
                            val date: String,
                            val truncatedText: String,
                            val highlightRange: IntRange,
                            val relevantLinkOrNull: NamedLink?
): SearchResult()

/**
 * Search result for snapshot title of which contains the search text.
 */
data class SnapshotSearchResult(val snapshot: Snapshot,
                                val titleHighlightRange: IntRange
): SearchResult()

abstract class SearchResult