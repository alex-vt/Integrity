/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.getArchivePath
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.getPageIndexLinks
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.saveLinkToIndex
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.savePageLinkToIndex
import com.alexvt.integrity.core.util.WebArchiveFilesUtil.webArchiveAlreadyDownloaded
import com.alexvt.integrity.core.util.WebViewUtil
import kotlinx.coroutines.isActive

internal abstract class CommonPaginationHelper {

    abstract suspend fun downloadPages(dl: BlogMetadataDownload): Boolean

    protected fun isRunning(dl: BlogMetadataDownload) = dl.jobContext.isActive

    protected suspend fun saveArchives(currentPageLink: String, additionalLinksOnPage: Set<String>,
                                       dl: BlogMetadataDownload,
                                       pageIndex: Int = getPaginationProgress(dl)) {
        val linksToArchive = linkedSetOf(currentPageLink)
                .plus(additionalLinksOnPage)
        linksToArchive.forEachIndexed { linkIndex, link -> run {
            if (!isRunning(dl)) return
            if (!webArchiveAlreadyDownloaded(link, dl.snapshotPath)) {
                IntegrityCore.postProgress(dl.artifactId, dl.date,
                        "Saving web archive " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                WebViewUtil.saveArchive(webView = dl.webView, url = link,
                        webArchivePath = "${dl.snapshotPath}/${getArchivePath(pageIndex, linkIndex)}",
                        loadIntervalMillis = dl.metadata.loadIntervalMillis,
                        loadImages = dl.metadata.loadImages,
                        desktopSite = dl.metadata.desktopSite)
                if (!isRunning(dl)) return
                saveLinkToIndex(link, dl.snapshotPath, pageIndex, linkIndex)
            }
        } }
    }

    /**
     * Adds a pagination page link to its web archive to the index file if still not there.
     */
    protected fun persistPaginationProgress(pageLink: String, dl: BlogMetadataDownload) {
        if (!isRunning(dl)) return
        val pageIndexLinks = getPageIndexLinks(dl.snapshotPath)
        if (!pageIndexLinks.contains(pageLink)) {
            savePageLinkToIndex(pageLink, dl.snapshotPath, pageIndexLinks.size)
        }
    }

    protected abstract fun getPaginationCount(pageLink: String, dl: BlogMetadataDownload): Int

    /**
     * Gets pagination progress starting from 0.
     */
    protected abstract fun getPaginationProgress(dl: BlogMetadataDownload): Int

    protected fun getPaginationProgressText(pageLink: String, dl: BlogMetadataDownload): String {
        val forPageText = "for page: $pageLink"
        val userVisibleIndex = getPaginationProgress(dl) + 1
        val count = getPaginationCount(pageLink, dl)
        return when {
            count > 1 -> "$forPageText\n($userVisibleIndex of $count)"
            else -> forPageText
        }
    }

}
