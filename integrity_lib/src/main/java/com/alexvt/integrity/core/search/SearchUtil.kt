/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

import com.alexvt.integrity.core.IntegrityCore
import java.util.regex.Pattern

object SearchUtil {

    fun searchText(searchedText: String, artifactId: Long?) = IntegrityCore.searchIndexRepository
            .searchText(searchedText)
            .filter { if (artifactId != null) it.artifactId == artifactId else true }
            .map { chunk -> getLocationsOfSearchedText(chunk.text, searchedText)
                    .map { range -> run {
                        val truncatedTextWithHighlightRange = truncateTextRange(chunk.text, range)
                        SearchResult(getSearchResultTitle(chunk), chunk.date,
                                truncatedTextWithHighlightRange.first,
                                truncatedTextWithHighlightRange.second,
                                getRelevantLinkOrNull(chunk.links, searchedText))
                    } }
            }.flatMap { it.toList() }

    private fun getLocationsOfSearchedText(text: String, searchedText: String)
            = Pattern.quote(searchedText).toRegex().findAll(text).map { it.range }

    private fun getSearchResultTitle(chunk: DataChunk) = IntegrityCore.metadataRepository
            .getSnapshotMetadata(chunk.artifactId, chunk.date).title

    private fun truncateTextRange(text: String, range: IntRange): Pair<String, IntRange> {
        val maxMargin = 100
        val truncatedTextStart = Math.max(0, range.first - maxMargin)
        val truncatedText = text
                .take(range.last + maxMargin)
                .removeRange(0, truncatedTextStart)
                .trim()
        return Pair(truncatedText,
                IntRange(range.first - truncatedTextStart, range.last - truncatedTextStart))
    }

    /**
     * Link relevance order:
     * 1. Link text contains searched text.
     * 2. Link address contains searched text.
     * 3. First link.
     */
    private fun getRelevantLinkOrNull(links: List<NamedLink>, searchedText: String)
            = links.find { it.title.contains(searchedText) }
            ?: links.find { it.link.contains(searchedText) }
            ?: links.firstOrNull()

}