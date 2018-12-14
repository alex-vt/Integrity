/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.getRelatedPageLinks
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.isRunning
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.saveArchives
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.saveLinks

internal object LinkedPaginationHelper {

    /**
     * Linked pagination:
     * do obtain page to get related links and possible next page link,
     * then page is archived,
     * continue while next page can be determined from metadata and current page contents.
     *
     * Starts from the provided main page.
     */
    suspend fun loadPages(dl: BlogTypeUtil.BlogMetadataDownload) = loadPages(dl.metadata.url, dl)

    private tailrec suspend fun loadPages(currentPageLink: String,
                                          dl: BlogTypeUtil.BlogMetadataDownload): Boolean {
        if (!dl.metadata.paginationUsed || dl.metadata.pagination !is LinkedPagination) {
            return false // no linked pagination
        }
        if (!isRunning(dl)) {
            return true // interrupted - no more pages
        }
        Log.d("LinkedPaginationHelper", "loadPages: $currentPageLink")
        val nextPageLinkPattern = dl.metadata.pagination.pathPrefix
        val relatedPageLinks = getRelatedPageLinks(currentPageLink, dl, nextPageLinkPattern)
        // todo don't save archives and links for next page yet
        saveArchives(currentPageLink, relatedPageLinks, dl)
        saveLinks(currentPageLink, relatedPageLinks, dl)
        if (!hasNextPageLink(relatedPageLinks, nextPageLinkPattern, dl)) {
            return true // no more pages exist or allowed
        }
        return loadPages(getNextPageLink(relatedPageLinks, nextPageLinkPattern), dl)
    }


    /**
     * Determines if there's another page in case of linked pagination.
     * True if provided pages contain pattern for next page.
     */
    private fun hasNextPageLink(relatedPageLinks: Set<String>, nextPageLinkPattern: String,
                                dl: BlogTypeUtil.BlogMetadataDownload)
            = dl.metadata.pagination is LinkedPagination
            && CommonPaginationHelper.getPreviousPageLinks(dl.snapshotPath).size < dl.metadata.pagination.limit
            && relatedPageLinks.any { it.contains(nextPageLinkPattern) }

    private fun getNextPageLink(relatedPageLinks: Set<String>, nextPageLinkPattern: String)
            = relatedPageLinks.first { it.contains(nextPageLinkPattern) }

}