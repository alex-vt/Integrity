/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.types.blog

import com.alexvt.integrity.core.data.types.blog.BlogMetadataDownload
import com.alexvt.integrity.lib.core.util.LinkUtil
import com.alexvt.integrity.lib.core.util.WebArchiveFilesUtil
import com.alexvt.integrity.core.data.types.blog.LinkedPagination
import com.alexvt.integrity.lib.core.data.filesystem.FileRepository
import com.alexvt.integrity.lib.core.operations.snapshots.DownloadProgressReporter
import com.alexvt.integrity.lib.core.util.WebPageLoader

internal class LinkedPaginationHelper(
        override val fileRepository: FileRepository,
        override val webArchiveFilesUtil: WebArchiveFilesUtil,
        override val webPageLoader: WebPageLoader,
        override val downloadProgressReporter: DownloadProgressReporter
) : CommonPaginationHelper(fileRepository, webArchiveFilesUtil, webPageLoader,
        downloadProgressReporter) {

    /**
     * Entry point for linked pagination.
     */
    override fun downloadPages(dl: BlogMetadataDownload)
            = downloadPages(getStartPage(dl), dl)

    /**
     * Gets first page to start downloading from.
     * If downloading is resuming, the last link from pagination index file is used.
     * Otherwise the first page is the main page.
     */
    private fun getStartPage(dl: BlogMetadataDownload)
            = webArchiveFilesUtil.getPageIndexLinks(dl.snapshotPath).lastOrNull() ?: dl.metadata.url

    /**
     * Recursive linked pagination:
     * do obtain page to build related links and possible next page link,
     * then page is archived,
     * continue while next page can be determined from metadata and current page contents.
     *
     * Pagination position is saved before page archiving:
     * on resume getPageIndexLinks will return the page at pagination position.
     */
    private tailrec fun downloadPages(currentPageLink: String,
                                              dl: BlogMetadataDownload): Boolean {
        if (!dl.metadata.paginationUsed || dl.metadata.pagination !is LinkedPagination) {
            return false // no linked pagination
        }
        if (!isRunning(dl)) {
            return true // interrupted - no more pages
        }
        persistPaginationProgress(currentPageLink, dl)
        val pageContents = getPageContents(currentPageLink, dl) // always needed to look for next page link
        val additionalLinksOnPage = getAdditionalLinksOnPage(currentPageLink, pageContents, dl)
        saveArchivesAndAddToSearchIndex(currentPageLink, additionalLinksOnPage, dl)
        if (!hasNextPageLink(pageContents, dl)) {
            return true // no more pages exist or allowed
        }
        return downloadPages(getNextPageLink(pageContents, dl), dl)
    }

    private fun getPageContents(currentPageLink: String, dl: BlogMetadataDownload): String {
        downloadProgressReporter.reportSnapshotDownloadProgress(dl.artifactId, dl.date,
                "Looking for links\n${getPaginationProgressText(currentPageLink, dl)}")
        val contents = webPageLoader.getHtml(currentPageLink, dl.metadata.loadImages,
                dl.metadata.desktopSite, null, null, dl.metadata.loadIntervalMillis)
        return contents
    }

    private fun getAdditionalLinksOnPage(currentPageLink: String, currentPageHtml: String,
                                         dl: BlogMetadataDownload) =
            if (dl.metadata.relatedPageLinksUsed) {
                val linksIncludingNextPage = LinkUtil.ccsSelectLinks(currentPageHtml,
                        dl.metadata.relatedPageLinksPattern,
                        dl.metadata.relatedPageLinksFilter, currentPageLink)
                        .keys
                        .minus(currentPageLink)
                val linksWithoutNextPage = if (hasNextPageLink(currentPageHtml, dl)) {
                    linksIncludingNextPage.minus(getNextPageLink(currentPageHtml, dl))
                } else {
                    linksIncludingNextPage
                }
                linksWithoutNextPage
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
        val pageIndexArchiveLinks = webArchiveFilesUtil.getPageIndexArchiveLinks(dl.snapshotPath)
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
        return webArchiveFilesUtil.getPageIndexArchiveLinks(dl.snapshotPath).size < dl.metadata.pagination.limit
                && nextPageLinks.isNotEmpty()
    }

    private fun getNextPageLink(currentPageHtml: String, dl: BlogMetadataDownload)
            = LinkUtil.getMatchedLinks(currentPageHtml,
            (dl.metadata.pagination as LinkedPagination).nextPageLinkFilter).last() // todo resolve multiple

}