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

internal abstract class CommonPaginationHelper {

    abstract suspend fun downloadPages(dl: BlogMetadataDownload): Boolean

    protected fun isRunning(dl: BlogMetadataDownload) = dl.jobContext.isActive

    protected suspend fun saveArchives(currentPageLink: String, relatedPageLinks: Set<String>,
                             dl: BlogMetadataDownload) {
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

    protected abstract fun getPaginationCount(pageLink: String, dl: BlogMetadataDownload): Int

    protected fun getPreviousPageLinks(snapshotPath: String)
            = getLinksFromFile(getPaginationPath(snapshotPath))

    protected fun saveLinks(pageLink: String, relatedPageLinks: Collection<String>,
                  dl: BlogMetadataDownload) {
        if (!isRunning(dl)) {
            return
        }
        Log.d("CommonPaginationHelper", "Saving links for page $pageLink")
        addTextsToFile(setOf(pageLink).plus(relatedPageLinks),
                getLinkRelatedLinksPath(dl.snapshotPath))
        addTextsToFile(setOf(pageLink),
                getPaginationPath(dl.snapshotPath))
    }


    protected fun getPaginationProgressText(pageLink: String,
                                            dl: BlogMetadataDownload): String {
        val forPageText = "for page: $pageLink"
        val index = getPreviousPageLinks(dl.snapshotPath).size + 1
        val count = getPaginationCount(pageLink, dl)
        return when {
            count > 1 -> "$forPageText\n($index of $count)"
            else -> forPageText
        }
    }

    private fun isFirstPage(snapshotPath: String) = getPreviousPageLinks(snapshotPath).isEmpty()


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
