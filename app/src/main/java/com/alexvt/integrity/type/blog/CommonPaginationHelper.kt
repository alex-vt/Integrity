/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.util.LinkUtil
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil.getArchivePath
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil.getPageIndexLinks
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil.saveLinkToIndex
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil.savePageLinkToIndex
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil.webArchiveAlreadyDownloaded
import com.alexvt.integrity.lib.util.WebPageLoader

internal abstract class CommonPaginationHelper {

    abstract fun downloadPages(dl: BlogMetadataDownload): Boolean

    protected fun isRunning(dl: BlogMetadataDownload)
            = IntegrityEx.isSnapshotDownloadRunning(dl.artifactId, dl.date)

    protected fun saveArchivesAndAddToSearchIndex(currentPageLink: String,
                                                  additionalLinksOnPage: Set<String>,
                                                  dl: BlogMetadataDownload,
                                                  pageIndex: Int = getPaginationProgress(dl)) {
        val linksToArchive = linkedSetOf(currentPageLink)
                .plus(additionalLinksOnPage)
        linksToArchive.forEachIndexed { linkIndex, link -> run {
            if (!isRunning(dl)) return
            if (!webArchiveAlreadyDownloaded(dl.context, link, dl.snapshotPath)) {
                IntegrityEx.reportSnapshotDownloadProgress(dl.context, dl.artifactId, dl.date,
                        "Saving web archive " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                val webArchivePath = "${dl.snapshotPath}/${getArchivePath(pageIndex, linkIndex)}"
                android.util.Log.v("WebPageLoader", "getHtmlAndSaveArchive, url = $link")
                val pageHtml = WebPageLoader().getHtmlAndSaveArchive(context = dl.context, url = link,
                        loadImages = dl.metadata.loadImages, desktopSite = dl.metadata.desktopSite,
                        archiveSavePath = webArchivePath,
                        delayMillis = dl.metadata.loadIntervalMillis)

                if (!isRunning(dl)) return
                saveLinkToIndex(dl.context, link, dl.snapshotPath, pageIndex, linkIndex)
                IntegrityEx.reportSnapshotDownloadProgress(dl.context, dl.artifactId, dl.date,
                        "Indexing page text " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                IntegrityEx.addDataForSearchIndex(dl.context, dl.artifactId, dl.date,
                        LinkUtil.getVisibleTextWithLinks(pageHtml), "${pageIndex}_$linkIndex",
                        "Page archive" to "file://$webArchivePath")
            }
        } }
    }

    /**
     * Adds a pagination page link to its web archive to the index file if still not there.
     */
    protected fun persistPaginationProgress(pageLink: String, dl: BlogMetadataDownload) {
        if (!isRunning(dl)) return
        val pageIndexLinks = getPageIndexLinks(dl.context, dl.snapshotPath)
        if (!pageIndexLinks.contains(pageLink)) {
            savePageLinkToIndex(dl.context, pageLink, dl.snapshotPath, pageIndexLinks.size)
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
