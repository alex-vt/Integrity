/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal object CommonPaginationHelper {

    fun isRunning(dl: BlogTypeUtil.BlogMetadataDownload) = dl.jobContext.isActive

    suspend fun saveArchives(currentPageLink: String, relatedPageLinks: Set<String>,
                             dl: BlogTypeUtil.BlogMetadataDownload) {
        if (!isRunning(dl)) {
            return
        }
        val linksToArchive = setOf(currentPageLink).plus(relatedPageLinks)
        WebViewUtil.saveArchives(dl.webView, linksToArchive, dl.snapshotPath,
                isFirstPage(dl.snapshotPath),
                getPaginationProgressText(currentPageLink, dl),
                dl.metadata.loadIntervalMillis, dl.metadata.loadImages,
                dl.metadata.desktopSite, dl.jobProgressListener, dl.jobContext)
    }

    /**
     * Gets links from page based on its currentPageUrl,
     * linkPattern to match links with (must contain it),
     * and possible cssSelector in metadata.
     */
    suspend fun getRelatedPageLinks(currentPageUrl: String, dl: BlogTypeUtil.BlogMetadataDownload,
                                    linkPattern: String = ""): Set<String> {
        IntegrityCore.postProgress(dl.jobProgressListener,
                "Collecting links\n${getPaginationProgressText(currentPageUrl, dl)}")
        val relatedPageUrls = linkedSetOf<String>()
        if (dl.metadata.relatedPageLinksUsed || linkPattern.isNotBlank()) {
            val pageHtml = WebViewUtil.loadHtml(dl.webView, currentPageUrl, dl.metadata.loadImages,
                    dl.metadata.desktopSite, setOf())
            delay(dl.metadata.loadIntervalMillis)
            if (dl.metadata.relatedPageLinksUsed) {
                val selectedLinkMap = LinkUtil.getCssSelectedLinkMap(pageHtml,
                        dl.metadata.relatedPageLinksPattern, currentPageUrl)
                Log.d("BlogDataTypeUtil", "getRelatedPageLinks: selectRelatedLinks" +
                        " = ${selectedLinkMap.keys}")
                relatedPageUrls.addAll(selectedLinkMap.keys)
            }
            if (linkPattern.isNotBlank()) {
                val matchedLinkMap = LinkUtil.getMatchedLinks(pageHtml, linkPattern)
                Log.d("BlogDataTypeUtil", "getRelatedPageLinks: getMatchedLinks" +
                        " = $matchedLinkMap")
                relatedPageUrls.addAll(matchedLinkMap)
            }
        }
        Log.d("BlogDataTypeUtil", "getRelatedPageLinks: Obtained ${relatedPageUrls.size} links" +
                " (related links selected: ${dl.metadata.relatedPageLinksUsed})" +
                " by pattern ${dl.metadata.relatedPageLinksPattern} \nat page $currentPageUrl")
        return relatedPageUrls.minus(currentPageUrl)
    }
    
    private fun getPaginationProgressText(pageLink: String,
                                          dl: BlogTypeUtil.BlogMetadataDownload): String {
        val forPageText = "for page: $pageLink"
        val index = getPreviousPageLinks(dl.snapshotPath).size + 1
        val count = if (dl.metadata.pagination is LinkedPagination) {
            dl.metadata.pagination.limit
        } else {
            IndexedPaginationHelper.getAllPageLinks(dl.metadata).size
        }
        if (count > 1) {
            return "$forPageText\n($index of $count)"
        }
        return forPageText
    }

    private fun isFirstPage(snapshotPath: String) = getPreviousPageLinks(snapshotPath).isEmpty()

    fun getPreviousPageLinks(snapshotPath: String)
            = getLinksFromFile(getPaginationPath(snapshotPath))

    fun saveLinks(pageLink: String, relatedPageLinks: Collection<String>,
                  dl: BlogTypeUtil.BlogMetadataDownload) {
        if (!isRunning(dl)) {
            return
        }
        Log.d("CommonPaginationHelper", "Saving links for page $pageLink")
        addTextsToFile(setOf(pageLink).plus(relatedPageLinks),
                getLinkRelatedLinksPath(dl.snapshotPath))
        addTextsToFile(setOf(pageLink),
                getPaginationPath(dl.snapshotPath))
    }


    // file operations

    private fun getPaginationPath(snapshotPath: String) = "$snapshotPath/pagination.txt"

    private fun getLinkRelatedLinksPath(snapshotPath: String) = "$snapshotPath/links.txt"

    private fun addTextsToFile(texts: Collection<String>, fullPath: String)
            = DataCacheFolderUtil.addTextToFile(texts.joinToString("\n"), fullPath)

    private fun getLinksFromFile(fullPath: String)
            = DataCacheFolderUtil.readTextFromFile(fullPath)
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

}