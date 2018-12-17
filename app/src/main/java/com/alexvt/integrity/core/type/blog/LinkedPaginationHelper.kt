/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import kotlinx.coroutines.delay

internal class LinkedPaginationHelper : CommonPaginationHelper() {

    /**
     * Linked pagination:
     * do obtain page to get related links and possible next page link,
     * then page is archived,
     * continue while next page can be determined from metadata and current page contents.
     *
     * Starts from the provided main page.
     */
    override suspend fun downloadPages(dl: BlogMetadataDownload) = loadPages(dl.metadata.url, dl)

    private tailrec suspend fun loadPages(currentPageLink: String,
                                          dl: BlogMetadataDownload): Boolean {
        if (!dl.metadata.paginationUsed || dl.metadata.pagination !is LinkedPagination) {
            return false // no linked pagination
        }
        if (!isRunning(dl)) {
            return true // interrupted - no more pages
        }
        Log.d("LinkedPaginationHelper", "downloadPages: $currentPageLink")
        val pageContents = getPageContents(currentPageLink, dl) // always needed to look for next page link
        val additionalLinksOnPage = getAdditionalLinksOnPage(currentPageLink, pageContents, dl)
        saveArchives(currentPageLink, additionalLinksOnPage, dl)
        saveLinks(currentPageLink, additionalLinksOnPage, dl)
        if (!hasNextPageLink(pageContents, dl)) {
            return true // no more pages exist or allowed
        }
        return loadPages(getNextPageLink(pageContents, dl), dl)
    }

    private suspend fun getPageContents(currentPageLink: String, dl: BlogMetadataDownload): String {
        IntegrityCore.postProgress(dl.jobProgressListener,
                "Looking for links\n${getPaginationProgressText(currentPageLink, dl)}")
        val contents = WebViewUtil.loadHtml(dl.webView, currentPageLink, dl.metadata.loadImages,
                dl.metadata.desktopSite, setOf())
        delay(dl.metadata.loadIntervalMillis)
        return contents
    }

    private fun getAdditionalLinksOnPage(currentPageLink: String, currentPageHtml: String,
                                         dl: BlogMetadataDownload) =
            if (dl.metadata.relatedPageLinksUsed) {
                LinkUtil.ccsSelectLinksInSameDomain(currentPageHtml,
                        dl.metadata.relatedPageLinksPattern, currentPageLink)
                        .keys
                        .minus(getNextPageLink(currentPageHtml, dl)) // next page link is not additional
            } else {
                setOf()
            }

    override fun getPaginationCount(pageLink: String, dl: BlogMetadataDownload)
            = (dl.metadata.pagination as LinkedPagination).limit

    /**
     * Determines if there's another page in case of linked pagination.
     * True if provided HTML contains link for next page.
     */
    private fun hasNextPageLink(currentPageHtml: String, dl: BlogMetadataDownload)
            = getPreviousPageLinks(dl.snapshotPath).size <
            (dl.metadata.pagination as LinkedPagination).limit
            && LinkUtil.getMatchedLinks(currentPageHtml, dl.metadata.pagination.pathPrefix)
            .isNotEmpty()

    private fun getNextPageLink(currentPageHtml: String, dl: BlogMetadataDownload)
            = LinkUtil.getMatchedLinks(currentPageHtml,
            (dl.metadata.pagination as LinkedPagination).pathPrefix).first()

}