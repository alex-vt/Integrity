/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.types.blog

import com.alexvt.integrity.core.data.types.blog.BlogMetadataDownload
import com.alexvt.integrity.lib.core.operations.search.SearchIndexAdder
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.data.jobs.GlobalRunningJobs
import com.alexvt.integrity.lib.core.util.LinkUtil
import com.alexvt.integrity.lib.core.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.core.data.filesystem.FileRepository
import com.alexvt.integrity.lib.core.operations.snapshots.DownloadProgressReporter
import com.alexvt.integrity.lib.core.util.WebPageLoader

internal abstract class CommonPaginationHelper(
        open val fileRepository: FileRepository,
        open val webArchiveFilesUtil: WebArchiveFilesUtil,
        open val webPageLoader: WebPageLoader,
        open val downloadProgressReporter: DownloadProgressReporter
) {

    abstract fun downloadPages(dl: BlogMetadataDownload): Boolean

    protected fun isRunning(dl: BlogMetadataDownload)
            = GlobalRunningJobs.RUNNING_JOB_REPOSITORY.isRunning(dl.artifactId, dl.date)

    protected fun saveArchivesAndAddToSearchIndex(currentPageLink: String,
                                                  additionalLinksOnPage: Set<String>,
                                                  dl: BlogMetadataDownload,
                                                  pageIndex: Int = getPaginationProgress(dl)) {
        val linksToArchive = linkedSetOf(currentPageLink)
                .plus(additionalLinksOnPage)
        linksToArchive.forEachIndexed { linkIndex, link -> run {
            if (!isRunning(dl)) return
            if (!webArchiveFilesUtil.webArchiveAlreadyDownloaded(link, dl.snapshotPath)) {
                downloadProgressReporter.reportSnapshotDownloadProgress(dl.artifactId, dl.date,
                        "Saving web archive " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                val webArchivePath = "${dl.snapshotPath}/${webArchiveFilesUtil.getArchivePath(pageIndex, linkIndex)}"
                val pageHtml = webPageLoader.getHtml(url = link,
                        loadImages = dl.metadata.loadImages, desktopSite = dl.metadata.desktopSite,
                        archiveSavePath = webArchivePath, screenshotSavePath = null,
                        delayMillis = dl.metadata.loadIntervalMillis)

                if (!isRunning(dl)) return
                webArchiveFilesUtil.saveLinkToIndex(link, dl.snapshotPath, pageIndex, linkIndex)
                downloadProgressReporter.reportSnapshotDownloadProgress(dl.artifactId, dl.date,
                        "Indexing page text " + (linkIndex + 1) + " of "
                                + linksToArchive.size + "\n"
                                + getPaginationProgressText(currentPageLink, dl))
                SearchIndexAdder(DataFolderManager(fileRepository))
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
