/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.operations.types.blog

import com.alexvt.integrity.core.data.types.blog.BlogMetadataDownload
import com.alexvt.integrity.lib.android.operations.snapshots.DataTypeService
import com.alexvt.integrity.lib.core.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.android.util.AndroidWebPageLoader
import com.alexvt.integrity.core.data.types.blog.BlogTypeMetadata
import com.alexvt.integrity.core.operations.types.blog.IndexedPaginationHelper
import com.alexvt.integrity.core.operations.types.blog.LinkedPaginationHelper
import com.alexvt.integrity.lib.android.data.filesystem.AndroidFileRepository
import com.alexvt.integrity.lib.android.operations.snapshots.AndroidDownloadProgressReporter
import com.alexvt.integrity.lib.core.data.filesystem.FileRepository
import com.alexvt.integrity.lib.core.operations.snapshots.DownloadProgressReporter
import com.alexvt.integrity.lib.core.util.WebPageLoader

class BlogTypeService: DataTypeService<BlogTypeMetadata>() {

    private val fileRepository: FileRepository by lazy {
        AndroidFileRepository(this)
    }
    private val webArchiveFilesUtil: WebArchiveFilesUtil by lazy {
        WebArchiveFilesUtil(dataFolderManager, logger)
    }
    private val webPageLoader: WebPageLoader by lazy {
        AndroidWebPageLoader(this)
    }
    private val downloadProgressReporter: DownloadProgressReporter by lazy {
        AndroidDownloadProgressReporter(this)
    }

    override fun getTypeMetadataClass() = BlogTypeMetadata::class.java


    override fun downloadData(dataFolderName: String, artifactId: Long, date: String,
                              typeMetadata: BlogTypeMetadata): String {
        val snapshotPath = dataFolderManager.getSnapshotFolderPath(dataFolderName, artifactId, date)

        val dl = BlogMetadataDownload(
                dataFolderName = dataFolderName,
                artifactId = artifactId,
                date = date,
                metadata = typeMetadata,
                snapshotPath = snapshotPath // todo remove as calculable
        )

        if (!LinkedPaginationHelper(fileRepository, webArchiveFilesUtil, webPageLoader,
                        downloadProgressReporter).downloadPages(dl)) {
            IndexedPaginationHelper(fileRepository, webArchiveFilesUtil, webPageLoader,
                    downloadProgressReporter).downloadPages(dl)
        }

        return snapshotPath
    }

    /**
     * Gets first saved page screenshot.
     */
    override fun generateOfflinePreview(dataFolderName: String, artifactId: Long, date: String,
                                        typeMetadata: BlogTypeMetadata) {
        val snapshotPath = dataFolderManager.getSnapshotFolderPath(dataFolderName,
                artifactId, date)
        val firstPageArchiveLink = webArchiveFilesUtil
                .getPageIndexArchiveLinks(snapshotPath).first()
        val snapshotPreviewPath = dataFolderManager.getSnapshotPreviewPath(dataFolderName,
                artifactId, date)

        webPageLoader.getHtml(
                "file://$snapshotPath/$firstPageArchiveLink", typeMetadata.loadImages,
                typeMetadata.desktopSite, null, snapshotPreviewPath,
                typeMetadata.loadIntervalMillis)
        }
}
