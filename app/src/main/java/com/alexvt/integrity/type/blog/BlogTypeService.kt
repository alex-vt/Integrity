/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import android.content.Context
import android.webkit.WebView
import com.alexvt.integrity.lib.DataTypeService
import com.alexvt.integrity.lib.IntegrityEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class BlogTypeService: DataTypeService<BlogTypeMetadata>() {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getTypeMetadataClass() = BlogTypeMetadata::class.java

    override fun getViewingActivityClass() = BlogTypeActivity::class.java


    override fun downloadData(artifactId: Long, date: String,
                              typeMetadata: BlogTypeMetadata): String {
        val snapshotPath = IntegrityEx.getSnapshotDataFolderPath(applicationContext, artifactId, date)

        runBlocking(Dispatchers.Main) {
            val dl = BlogMetadataDownload(
                    context = applicationContext,
                    artifactId = artifactId,
                    date = date,
                    webView = WebView(applicationContext),
                    metadata = typeMetadata,
                    snapshotPath = snapshotPath
            )

            if (!LinkedPaginationHelper().downloadPages(dl)) {
                IndexedPaginationHelper().downloadPages(dl)
            }
        }

        return snapshotPath
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
        val webView: WebView,
        val metadata: BlogTypeMetadata,
        val snapshotPath: String
)