/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.search

import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.data.search.SearchIndexRepository
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.search.*
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.util.regex.Pattern
import javax.inject.Inject

class SearchManager @Inject constructor(private val metadataRepository: MetadataRepository,
                                        private val searchIndexRepository: SearchIndexRepository) {

    /**
     * Searches snapshot titles and indexed text data, then merges search results.
     */
    fun search(searchRequest: SearchRequest)
            = searchSnapshotTitles(searchRequest)
            .zipWith(searchText(searchRequest), BiFunction {
                snapshotResults: List<SearchResult>, textResults: List<SearchResult> ->
                snapshotResults.plus(textResults)
            })

    fun searchSnapshotTitles(searchRequest: SearchRequest)
            = searchSnapshots(searchRequest)
            .map { snapshotToSearchResults(searchRequest.text, it) }
            .map { SortingUtil.sort(it, searchRequest.sortingMethod) }
            .map { if (searchRequest.onePerArtifact) {
                it.distinctBy { it.snapshot.artifactId }
            } else {
                it
            }}

    private fun snapshotToSearchResults(searchedText: String, snapshots: List<Snapshot>) = snapshots
            .map { snapshot -> getLocationsOfSearchedText(snapshot.title, searchedText)
                    .map { range -> SnapshotSearchResult(snapshot, range) }
            }.flatMap { it.toList() }

    private fun searchSnapshots(searchRequest: SearchRequest) =
            if (isBigEnough(searchRequest.text)) {
                if (searchRequest.artifactId != null) {
                    metadataRepository.searchTitleSingle(searchRequest.text, searchRequest.artifactId!!)
                } else {
                    metadataRepository.searchTitleSingle(searchRequest.text)
                }
            } else {
                Single.just(emptyList())
            }

    fun searchText(searchRequest: SearchRequest)
            = searchIndexedChunks(searchRequest)
            .map { chunksToSearchResults(searchRequest.text, it) }
            .map { SortingUtil.sort(it, searchRequest.sortingMethod) }
            .map { if (searchRequest.onePerArtifact) {
                it.distinctBy { it.artifactId }
            } else {
                it
            }}

    private val minSearchTextLength = 3

    private fun isBigEnough(searchText: String) = searchText.length >= minSearchTextLength

    private fun searchIndexedChunks(searchRequest: SearchRequest) =
            if (isBigEnough(searchRequest.text)) {
                if (searchRequest.artifactId != null) {
                    searchIndexRepository.searchTextSingle(searchRequest.text, searchRequest.artifactId!!)
                } else {
                    searchIndexRepository.searchTextSingle(searchRequest.text)
                }
            } else {
                Single.just(emptyList())
            }

    private fun chunksToSearchResults(searchedText: String, chunks: List<DataChunk>) = chunks
            .map { chunk -> getLocationsOfSearchedText(chunk.text, searchedText)
                    .map { range -> run {
                        val truncatedTextWithHighlightRange = truncateTextRange(chunk.text, range)
                        TextSearchResult(getSearchResultTitle(chunk), chunk.artifactId,
                                chunk.date, truncatedTextWithHighlightRange.first,
                                truncatedTextWithHighlightRange.second,
                                getRelevantLinkOrNull(chunk.links, searchedText))
                    }
                    }
            }.flatMap { it.toList() }

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