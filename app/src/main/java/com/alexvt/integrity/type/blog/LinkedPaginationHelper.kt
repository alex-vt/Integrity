/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.LinkUtil
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.getPageIndexArchiveLinks
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.getPageIndexLinks
import com.alexvt.integrity.core.util.WebViewUtil
import kotlinx.coroutines.delay

internal class LinkedPaginationHelper : CommonPaginationHelper() {

    /**
     * Entry point for linked pagination.
     */
    override suspend fun downloadPages(dl: BlogMetadataDownload)
            = downloadPages(getStartPage(dl), dl)

    /**
     * Gets first page to start downloading from.
     * If downloading is resuming, the last link from pagination index file is used.
     * Otherwise the first page is the main page.
     */
    private fun getStartPage(dl: BlogMetadataDownload)
            = getPageIndexLinks(dl.snapshotPath).lastOrNull() ?: dl.metadata.url

    /**
     * Recursive linked pagination:
     * do obtain page to get related links and possible next page link,
     * then page is archived,
     * continue while next page can be determined from metadata and current page contents.
     *
     * Pagination position is saved before page archiving:
     * on resume getPageIndexLinks will return the page at pagination position.
     */
    private tailrec suspend fun downloadPages(currentPageLink: String,
                                              dl: BlogMetadataDownload): Boolean {
        if (!dl.metadata.paginationUsed || dl.metadata.pagination !is LinkedPagination) {
            return false // no linked pagination
        }
        if (!isRunning(dl)) {
            return true // interrupted - no more pages
        }
        persistPaginationProgress(currentPageLink, dl)
        Log.d("LinkedPaginationHelper", "downloadPages: $currentPageLink")
        val pageContents = getPageContents(currentPageLink, dl) // always needed to look for next page link
        val additionalLinksOnPage = getAdditionalLinksOnPage(currentPageLink, pageContents, dl)
        saveArchives(currentPageLink, additionalLinksOnPage, dl)
        if (!hasNextPageLink(pageContents, dl)) {
            return true // no more pages exist or allowed
        }
        return downloadPages(getNextPageLink(pageContents, dl), dl)
    }

    private suspend fun getPageContents(currentPageLink: String, dl: BlogMetadataDownload): String {
        IntegrityCore.postProgress(dl.artifactId, dl.date,
                "Looking for links\n${getPaginationProgressText(currentPageLink, dl)}")
        val contents = WebViewUtil.loadHtml(dl.webView, currentPageLink, dl.metadata.loadImages,
                dl.metadata.desktopSite)
        delay(dl.metadata.loadIntervalMillis)
        return contents
    }

    private fun getAdditionalLinksOnPage(currentPageLink: String, currentPageHtml: String,
                                         dl: BlogMetadataDownload) =
            if (dl.metadata.relatedPageLinksUsed) {
                LinkUtil.ccsSelectLinks(currentPageHtml, dl.metadata.relatedPageLinksPattern,
                        dl.metadata.relatedPageLinksFilter, currentPageLink)
                        .keys
                        .minus(currentPageLink)
                        .minus(getNextPageLink(currentPageHtml, dl)) // next page link is not additional
            } else {
                setOf()
            }

    override fun getPaginationCount(pageLink: String, dl: BlogMetadataDownload)
            = (dl.metadata.pagination as LinkedPagination).limit

    /**
     * Gets pagination progress starting from 0.
     *
     * As pagination position is saved before page archiving,
     * pagination progress is 1 less than pagination index size.
     */
    override fun getPaginationProgress(dl: BlogMetadataDownload): Int {
        val pageIndexArchiveLinks = getPageIndexArchiveLinks(dl.snapshotPath)
        if (pageIndexArchiveLinks.isNotEmpty()) {
            return pageIndexArchiveLinks.size - 1
        } else {
            return 0
        }
    }

    /**
     * Determines if there's another page in case of linked pagination.
     * True if provided HTML contains link for next page.
     */
    private fun hasNextPageLink(currentPageHtml: String, dl: BlogMetadataDownload): Boolean {
        val nextPageLinks = LinkUtil.getMatchedLinks(currentPageHtml,
                (dl.metadata.pagination as LinkedPagination).nextPageLinkFilter)
        Log.d("LinkedPaginationHelper", "hasNextPageLink: $nextPageLinks")
        return getPageIndexArchiveLinks(dl.snapshotPath).size < dl.metadata.pagination.limit
                && nextPageLinks.isNotEmpty()
    }

    private fun getNextPageLink(currentPageHtml: String, dl: BlogMetadataDownload)
            = LinkUtil.getMatchedLinks(currentPageHtml,
            (dl.metadata.pagination as LinkedPagination).nextPageLinkFilter).first()

}