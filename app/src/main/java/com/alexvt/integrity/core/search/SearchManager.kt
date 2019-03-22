/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.lib.search.DataChunk
import com.alexvt.integrity.lib.search.NamedLink
import com.alexvt.integrity.lib.search.SearchResult
import io.reactivex.Single
import java.util.regex.Pattern
import javax.inject.Inject

class SearchManager @Inject constructor(private val metadataRepository: MetadataRepository,
                                        private val searchIndexRepository: SearchIndexRepository) {

    fun searchText(searchedText: String, artifactId: Long?)
            = searchTextIfBigEnough(searchedText, artifactId)
            .map { chunksToSearchResults(searchedText, it) }

    private fun chunksToSearchResults(searchedText: String, chunks: List<DataChunk>)
            = chunks.map { chunk -> getLocationsOfSearchedText(chunk.text, searchedText)
            .map { range -> run {
                val truncatedTextWithHighlightRange = truncateTextRange(chunk.text, range)
                SearchResult(getSearchResultTitle(chunk), chunk.date,
                        truncatedTextWithHighlightRange.first,
                        truncatedTextWithHighlightRange.second,
                        getRelevantLinkOrNull(chunk.links, searchedText))
            }
            }
    }.flatMap { it.toList() }


    private val minSearchTextLength = 3

    private fun isBigEnough(searchText: String) = searchText.length >= minSearchTextLength

    private fun searchTextIfBigEnough(searchedText: String, artifactId: Long?) =
            if (isBigEnough(searchedText)) {
                if (artifactId != null) {
                    searchIndexRepository.searchTextSingle(searchedText, artifactId)
                } else {
                    searchIndexRepository.searchTextSingle(searchedText)
                }
            } else {
                Single.just(emptyList())
            }

    private fun getLocationsOfSearchedText(text: String, searchedText: String)
            = Pattern.quote(searchedText).toRegex().findAll(text).map { it.range }

    private fun getSearchResultTitle(chunk: DataChunk) = metadataRepository
            .getSnapshotMetadataBlocking(chunk.artifactId, chunk.date).title

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