/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import android.webkit.WebView
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.alexvt.integrity.core.type.DataTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext


class BlogTypeUtil: DataTypeUtil<BlogTypeMetadata> {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getOperationMainActivityClass() = BlogTypeActivity::class.java

    override suspend fun downloadData(artifactId: Long, date: String,
                                      blogMetadata: BlogTypeMetadata,
                                      jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
                                      jobContext: CoroutineContext): String {
        Log.d("BlogDataTypeUtil", "downloadData start")
        val snapshotPath = DataCacheFolderUtil.ensureSnapshotFolder(artifactId, date)

        runBlocking(Dispatchers.Main) {
            val dl = BlogMetadataDownload(
                    webView = WebView(IntegrityCore.context),
                    metadata = blogMetadata,
                    snapshotPath = snapshotPath,
                    jobProgressListener = jobProgressListener,
                    jobContext = jobContext
            )

            if (!LinkedPaginationHelper().downloadPages(dl)) {
                IndexedPaginationHelper().downloadPages(dl)
            }
        }

        Log.d("BlogDataTypeUtil", "downloadData end")
        return snapshotPath
    }
}

/**
 * Holds Blog Type metadata and properties of environment for its data downloading.
 *
 * Serves for downloading algorithm code clarity.
 */
internal data class BlogMetadataDownload(
        val webView: WebView,
        val metadata: BlogTypeMetadata,
        val snapshotPath: String,
        val jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
        val jobContext: CoroutineContext
)