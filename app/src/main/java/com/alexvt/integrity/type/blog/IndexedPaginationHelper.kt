/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import android.content.Context
import com.alexvt.integrity.lib.operations.SnapshotDownloadReporter
import com.alexvt.integrity.lib.util.LinkUtil
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebPageLoader

internal class IndexedPaginationHelper(override val webArchiveFilesUtil: WebArchiveFilesUtil)
    : CommonPaginationHelper(webArchiveFilesUtil) {

    /**
     * Indexed pagination or no pagination:
     * while page link can be determined from metadata and previous page link list,
     * this page is obtained when related links needed,
     * then page is archived.
     *
     * Pagination position is saved after page archiving:
     * on resume getNextPageLink will return the next page after the saved pagination position.
     */
    override fun downloadPages(dl: BlogMetadataDownload): Boolean {
        while (isRunning(dl) && hasNextPageLink(dl)) {
            val currentPageLink = getNextPageLink(dl)
            android.util.Log.v("IndexedPaginationHelper", "downloadPages: $currentPageLink")
            val additionalLinksOnPage = getAdditionalLinksOnPage(currentPageLink, dl)
            saveArchivesAndAddToSearchIndex(currentPageLink, additionalLinksOnPage, dl)
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
            = webArchiveFilesUtil.getPageIndexArchiveLinks(dl.snapshotPath).size

    private fun getAdditionalLinksOnPage(currentPageLink: String,
                                                 dl: BlogMetadataDownload): Set<String> {
        SnapshotDownloadReporter.reportSnapshotDownloadProgress(dl.context, dl.artifactId, dl.date,
                "Collecting links\n${getPaginationProgressText(currentPageLink, dl)}")
        return if (pageContentsNeeded(dl)) {
            val currentPageHtml = WebPageLoader().getHtml(dl.context, currentPageLink,
                    dl.metadata.loadImages, dl.metadata.desktopSite, null, null,
                    dl.metadata.loadIntervalMillis)
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
            = getNextPageLinks(dl.context, dl.metadata, dl.snapshotPath)
            .isNotEmpty()

    private fun getNextPageLink(dl: BlogMetadataDownload)
            = getNextPageLinks(dl.context, dl.metadata, dl.snapshotPath)
            .first()

    private fun getNextPageLinks(context: Context, blogMetadata: BlogTypeMetadata, snapshotPath: String)
            = getAllPageLinks(blogMetadata)
            .minus(webArchiveFilesUtil.getPageIndexLinks(snapshotPath))

}