/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.DataCacheFolderUtil
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
            if (!webArchiveAlreadyDownloaded(link, dl)) {
                IntegrityCore.postProgress(dl.jobProgressListener,
                        "Saving web archive " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                WebViewUtil.saveArchive(webView = dl.webView, url = link,
                        webArchivePath = getArchivePath(dl, pageIndex, linkIndex),
                        loadIntervalMillis = dl.metadata.loadIntervalMillis,
                        loadImages = dl.metadata.loadImages,
                        desktopSite = dl.metadata.desktopSite)
                saveLinkToIndex(link, dl, pageIndex, linkIndex)
            }
        } }
    }

    /**
     * Adds a pagination page link to its web archive to the index file if still not there.
     */
    protected fun persistPaginationProgress(pageLink: String, dl: BlogMetadataDownload) {
        val pageIndexLinks = getPageIndexLinks(dl.snapshotPath)
        if (!pageIndexLinks.contains(pageLink)) {
            savePageLinkToIndex(pageLink, dl, pageIndexLinks.size)
        }
    }

    protected abstract fun getPaginationCount(pageLink: String, dl: BlogMetadataDownload): Int

    /**
     * Gets pagination progress starting from 0.
     */
    protected abstract fun getPaginationProgress(dl: BlogMetadataDownload): Int

    /**
     * Gets web archive links for already persisted pagination-to-web-archives index.
     */
    protected fun getPageIndexArchiveLinks(snapshotPath: String)
            = LinkUtil.getLinks(DataCacheFolderUtil.readTextFromFile(getPaginationPath(snapshotPath)))

    /**
     * Gets page links for already persisted pagination-to-web-archives index.
     */
    protected fun getPageIndexLinks(snapshotPath: String)
            = LinkUtil.getLinkTexts(DataCacheFolderUtil.readTextFromFile(getPaginationPath(snapshotPath)))

    protected fun getPaginationProgressText(pageLink: String, dl: BlogMetadataDownload): String {
        val forPageText = "for page: $pageLink"
        val userVisibleIndex = getPaginationProgress(dl) + 1
        val count = getPaginationCount(pageLink, dl)
        return when {
            count > 1 -> "$forPageText\n($userVisibleIndex of $count)"
            else -> forPageText
        }
    }


    private fun savePageLinkToIndex(pageLink: String, dl: BlogMetadataDownload, pageIndex: Int) {
        if (!isRunning(dl)) return
        Log.d("CommonPaginationHelper", "savePageLinkToIndex: $pageLink")
        DataCacheFolderUtil.addTextToFile(getLocalHtmlLink(pageLink, dl, pageIndex, 0),
                getPaginationPath(dl.snapshotPath))
    }

    private fun saveLinkToIndex(link: String, dl: BlogMetadataDownload, pageIndex: Int,
                                linkIndex: Int) {
        if (!isRunning(dl)) return
        Log.d("CommonPaginationHelper", "saveLinkToIndex: $link")
        DataCacheFolderUtil.addTextToFile(getLocalHtmlLink(link, dl, pageIndex, linkIndex),
                getLinksPath(dl.snapshotPath))
    }

    private fun getLocalHtmlLink(link: String, dl: BlogMetadataDownload, pageIndex: Int, linkIndex: Int)
            = "<h1><a href=\"${getArchivePath(dl, pageIndex, linkIndex)}\">\n$link\n</a></h1>\n<br/>"

    /**
     * Web archive is downloaded when its corresponding link exists in index.
     */
    private fun webArchiveAlreadyDownloaded(link: String, dl: BlogMetadataDownload)
            = LinkUtil.getLinkTexts(DataCacheFolderUtil.readTextFromFile(getLinksPath(dl.snapshotPath)))
            .contains(link)

    private fun getArchivePath(dl: BlogMetadataDownload, pageIndex: Int, linkIndex: Int)
            = "${dl.snapshotPath}/page${pageIndex}_link$linkIndex.mht"

    private fun getPaginationPath(snapshotPath: String) = "$snapshotPath/index-pages.html"

    private fun getLinksPath(snapshotPath: String) = "$snapshotPath/index.html"

}
