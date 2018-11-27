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


class BlogTypeUtil: DataTypeUtil<BlogTypeMetadata> {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getOperationMainActivityClass() = BlogTypeActivity::class.java

    override suspend fun downloadData(artifactId: Long, date: String,
                                      dataTypeSpecificMetadata: BlogTypeMetadata,
                                      jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit): String {
        Log.d("BlogDataTypeUtil", "downloadData start")

        jobProgressListener.invoke(JobProgress(
                progressMessage = "Downloading web pages data"
        ))

        val webView = WebView(IntegrityCore.context)
        val snapshotDataDirectory = DataCacheFolderUtil.createEmptyFolder(artifactId, date)

        val mainPageUrl = dataTypeSpecificMetadata.url
        val mainPageHtml = WebViewUtil.loadHtml(webView, mainPageUrl, setOf())

        val cssSelector = dataTypeSpecificMetadata.relatedPageLinksPattern

        try {
            val relatedPageUrls = LinkUtil.getCssSelectedLinkMap(mainPageHtml, cssSelector, mainPageUrl).keys

            val allTargetUrlToArchivePathMap = mapOf(mainPageUrl to (snapshotDataDirectory + "/index.mht"))
                    .plus(relatedPageUrls
                            .associate { it to (snapshotDataDirectory + "/page_" + it.hashCode() + ".mht")})

            WebViewUtil.saveArchives(webView, allTargetUrlToArchivePathMap, jobProgressListener)

            Log.d("BlogDataTypeUtil", "downloadData end")
        } catch (t: Throwable) {
            Log.e("BlogDataTypeUtil", "downloadData exception", t)
        }
        return snapshotDataDirectory
    }

}