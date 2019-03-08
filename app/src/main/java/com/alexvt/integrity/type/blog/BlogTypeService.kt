/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import android.content.Context
import com.alexvt.integrity.lib.DataTypeService
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebPageLoader
import com.alexvt.integrity.type.blog.ui.BlogTypeActivity

class BlogTypeService: DataTypeService<BlogTypeMetadata>() {

    private val webArchiveFilesUtil: WebArchiveFilesUtil by lazy {
        WebArchiveFilesUtil(dataFolderManager)
    }

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getTypeMetadataClass() = BlogTypeMetadata::class.java

    override fun getViewingActivityClass() = BlogTypeActivity::class.java


    override fun downloadData(dataFolderName: String, artifactId: Long, date: String,
                              typeMetadata: BlogTypeMetadata): String {
        val snapshotPath = dataFolderManager.getSnapshotFolderPath(dataFolderName, artifactId, date)

        val dl = BlogMetadataDownload(
                context = applicationContext,
                dataFolderName = dataFolderName,
                artifactId = artifactId,
                date = date,
                metadata = typeMetadata,
                snapshotPath = snapshotPath // todo remove as calculable
        )

        if (!LinkedPaginationHelper(webArchiveFilesUtil).downloadPages(dl)) {
            IndexedPaginationHelper(webArchiveFilesUtil).downloadPages(dl)
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

        WebPageLoader().getHtmlAndSaveScreenshot(applicationContext,
                "file://$snapshotPath/$firstPageArchiveLink", typeMetadata.loadImages,
                typeMetadata.desktopSite, snapshotPreviewPath, typeMetadata.loadIntervalMillis)
        }
}

/**
 * Holds Blog Type metadata and properties of environment for its data downloading.
 *
 * Serves for downloading algorithm code clarity.
 */
internal data class BlogMetadataDownload(
        val context: Context,
        val dataFolderName: String,
        val artifactId: Long,
        val date: String,
        val metadata: BlogTypeMetadata,
        val snapshotPath: String
)