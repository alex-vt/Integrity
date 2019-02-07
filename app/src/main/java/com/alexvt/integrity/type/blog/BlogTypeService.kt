/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import android.content.Context
import com.alexvt.integrity.lib.DataTypeService
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebPageLoader

class BlogTypeService: DataTypeService<BlogTypeMetadata>() {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getTypeMetadataClass() = BlogTypeMetadata::class.java

    override fun getViewingActivityClass() = BlogTypeActivity::class.java


    override fun downloadData(artifactId: Long, date: String,
                              typeMetadata: BlogTypeMetadata): String {
        val snapshotPath = IntegrityEx.getSnapshotDataFolderPath(applicationContext, artifactId, date)

        val dl = BlogMetadataDownload(
                context = applicationContext,
                artifactId = artifactId,
                date = date,
                metadata = typeMetadata,
                snapshotPath = snapshotPath
        )

        if (!LinkedPaginationHelper().downloadPages(dl)) {
            IndexedPaginationHelper().downloadPages(dl)
        }

        return snapshotPath
    }

    /**
     * Gets first saved page screenshot.
     */
    override fun generateOfflinePreview(artifactId: Long, date: String, typeMetadata: BlogTypeMetadata) {
        val snapshotPath = IntegrityEx.getSnapshotDataFolderPath(applicationContext, artifactId, date)
        val firstPageArchiveLink = WebArchiveFilesUtil.getPageIndexArchiveLinks(applicationContext,
                snapshotPath).first()
        val snapshotPreviewPath = IntegrityEx.getSnapshotPreviewPath(applicationContext, artifactId, date)

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
        val artifactId: Long,
        val date: String,
        val metadata: BlogTypeMetadata,
        val snapshotPath: String
)