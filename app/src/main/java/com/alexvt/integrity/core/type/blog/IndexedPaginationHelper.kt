/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.getPreviousPageLinks
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.getRelatedPageLinks
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.isRunning
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.saveArchives
import com.alexvt.integrity.core.type.blog.CommonPaginationHelper.saveLinks

internal object IndexedPaginationHelper {

    /**
     * Indexed pagination or no pagination:
     * while page link can be determined from metadata and previous page link list,
     * this page is obtained when related links needed,
     * then page is archived.
     */
    suspend fun loadPages(dl: BlogTypeUtil.BlogMetadataDownload) {
        while (isRunning(dl) && hasNextPageLink(dl)) {
            val currentPageLink = getNextPageLink(dl)
            Log.d("IndexedPaginationHelper", "loadPages: $currentPageLink")
            val relatedPageLinks = getRelatedPageLinks(currentPageLink, dl)
            saveArchives(currentPageLink, relatedPageLinks, dl)
            Log.d("IndexedPaginationHelper", "loadPages, saving links: $currentPageLink, $relatedPageLinks")
            saveLinks(currentPageLink, relatedPageLinks, dl)
        }
    }


    fun getAllPageLinks(blogMetadata: BlogTypeMetadata)
            = if (blogMetadata.paginationUsed && blogMetadata.pagination is IndexedPagination) {
        IntProgression.fromClosedRange(
                blogMetadata.pagination.startIndex,
                blogMetadata.pagination.limit,
                blogMetadata.pagination.step
        ).map { blogMetadata.url + blogMetadata.pagination.path + it }
    } else {
        listOf(blogMetadata.url)
    }

    private fun hasNextPageLink(dl: BlogTypeUtil.BlogMetadataDownload)
            = getNextPageLinks(dl.metadata, dl.snapshotPath)
            .isNotEmpty()

    private fun getNextPageLink(dl: BlogTypeUtil.BlogMetadataDownload)
            = getNextPageLinks(dl.metadata, dl.snapshotPath)
            .first()

    private fun getNextPageLinks(blogMetadata: BlogTypeMetadata, snapshotPath: String)
            = getAllPageLinks(blogMetadata)
            .minus(getPreviousPageLinks(snapshotPath))

}