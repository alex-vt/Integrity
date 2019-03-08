/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import com.alexvt.integrity.lib.filesystem.AndroidFilesystemManager
import com.alexvt.integrity.lib.search.SearchIndexManager
import com.alexvt.integrity.lib.operations.SnapshotDownloadReporter
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.IntegrityLib
import com.alexvt.integrity.lib.util.LinkUtil
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebPageLoader

internal abstract class CommonPaginationHelper(open val webArchiveFilesUtil: WebArchiveFilesUtil) {

    abstract fun downloadPages(dl: BlogMetadataDownload): Boolean

    protected fun isRunning(dl: BlogMetadataDownload)
            = IntegrityLib.runningJobManager.isRunning(dl.artifactId, dl.date)

    protected fun saveArchivesAndAddToSearchIndex(currentPageLink: String,
                                                  additionalLinksOnPage: Set<String>,
                                                  dl: BlogMetadataDownload,
                                                  pageIndex: Int = getPaginationProgress(dl)) {
        val linksToArchive = linkedSetOf(currentPageLink)
                .plus(additionalLinksOnPage)
        linksToArchive.forEachIndexed { linkIndex, link -> run {
            if (!isRunning(dl)) return
            if (!webArchiveFilesUtil.webArchiveAlreadyDownloaded(link, dl.snapshotPath)) {
                SnapshotDownloadReporter.reportSnapshotDownloadProgress(dl.context, dl.artifactId, dl.date,
                        "Saving web archive " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                val webArchivePath = "${dl.snapshotPath}/${webArchiveFilesUtil.getArchivePath(pageIndex, linkIndex)}"
                android.util.Log.v("WebPageLoader", "getHtmlAndSaveArchive, url = $link")
                val pageHtml = WebPageLoader().getHtmlAndSaveArchive(context = dl.context, url = link,
                        loadImages = dl.metadata.loadImages, desktopSite = dl.metadata.desktopSite,
                        archiveSavePath = webArchivePath,
                        delayMillis = dl.metadata.loadIntervalMillis)

                if (!isRunning(dl)) return
                webArchiveFilesUtil.saveLinkToIndex(link, dl.snapshotPath, pageIndex, linkIndex)
                SnapshotDownloadReporter.reportSnapshotDownloadProgress(dl.context, dl.artifactId, dl.date,
                        "Indexing page text " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                SearchIndexManager(DataFolderManager(AndroidFilesystemManager(dl.context)))
                        .addDataForSearchIndex(
                        dl.dataFolderName, dl.artifactId, dl.date,
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
        val pageIndexLinks = webArchiveFilesUtil.getPageIndexLinks(dl.snapshotPath)
        if (!pageIndexLinks.contains(pageLink)) {
            webArchiveFilesUtil.savePageLinkToIndex(pageLink, dl.snapshotPath, pageIndexLinks.size)
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
