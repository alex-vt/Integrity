/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.type.blog.WebArchiveFilesUtil.getPageIndexArchiveLinks
import com.alexvt.integrity.core.type.blog.WebArchiveFilesUtil.getPageIndexLinks
import kotlinx.coroutines.delay

internal class IndexedPaginationHelper : CommonPaginationHelper() {

    /**
     * Indexed pagination or no pagination:
     * while page link can be determined from metadata and previous page link list,
     * this page is obtained when related links needed,
     * then page is archived.
     *
     * Pagination position is saved after page archiving:
     * on resume getNextPageLink will return the next page after the saved pagination position.
     */
    override suspend fun downloadPages(dl: BlogMetadataDownload): Boolean {
        while (isRunning(dl) && hasNextPageLink(dl)) {
            val currentPageLink = getNextPageLink(dl)
            Log.d("IndexedPaginationHelper", "downloadPages: $currentPageLink")
            val additionalLinksOnPage = getAdditionalLinksOnPage(currentPageLink, dl)
            saveArchives(currentPageLink, additionalLinksOnPage, dl)
            persistPaginationProgress(currentPageLink, dl)
        }
        return true
    }

    override fun getPaginationCount(pageLink: String, dl: BlogMetadataDownload)
            = getAllPageLinks(dl.metadata).size

    /**
     * Gets pagination progress starting from 0.
     *
     * As pagination position is saved after page archiving,
     * progress index equals pagination position.
     */
    override fun getPaginationProgress(dl: BlogMetadataDownload)
            = getPageIndexArchiveLinks(dl.snapshotPath).size

    private suspend fun getAdditionalLinksOnPage(currentPageLink: String,
                                                 dl: BlogMetadataDownload): Set<String> {
        IntegrityCore.postProgress(dl.jobProgressListener,
                "Collecting links\n${getPaginationProgressText(currentPageLink, dl)}")
        return if (pageContentsNeeded(dl)) {
            val currentPageHtml = WebViewUtil.loadHtml(dl.webView, currentPageLink,
                    dl.metadata.loadImages, dl.metadata.desktopSite)
            delay(dl.metadata.loadIntervalMillis)
            LinkUtil.ccsSelectLinks(currentPageHtml, dl.metadata.relatedPageLinksPattern,
                    dl.metadata.relatedPageLinksFilter, currentPageLink)
                    .keys
                    .minus(currentPageLink)
        } else {
            linkedSetOf()
        }
    }

    private fun pageContentsNeeded(dl: BlogMetadataDownload) = dl.metadata.relatedPageLinksUsed

    private fun getAllPageLinks(blogMetadata: BlogTypeMetadata)
            = if (blogMetadata.paginationUsed && blogMetadata.pagination is IndexedPagination) {
        IntProgression.fromClosedRange(
                blogMetadata.pagination.startIndex,
                blogMetadata.pagination.limit,
                blogMetadata.pagination.step
        ).map { blogMetadata.url + blogMetadata.pagination.path + it }
    } else {
        listOf(blogMetadata.url) // only the start page
    }

    private fun hasNextPageLink(dl: BlogMetadataDownload)
            = getNextPageLinks(dl.metadata, dl.snapshotPath)
            .isNotEmpty()

    private fun getNextPageLink(dl: BlogMetadataDownload)
            = getNextPageLinks(dl.metadata, dl.snapshotPath)
            .first()

    private fun getNextPageLinks(blogMetadata: BlogTypeMetadata, snapshotPath: String)
            = getAllPageLinks(blogMetadata)
            .minus(getPageIndexLinks(snapshotPath))

}