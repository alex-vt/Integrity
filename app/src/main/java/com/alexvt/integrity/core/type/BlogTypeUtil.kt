/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type

import android.util.Log
import android.webkit.WebView
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlogTypeUtil: DataTypeUtil<BlogTypeMetadata> {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getOperationMainActivityClass() = BlogTypeActivity::class.java

    override suspend fun downloadData(artifactId: Long, date: String,
                                      dataTypeSpecificMetadata: BlogTypeMetadata,
                                      webViewWithContent: WebView): String {
        Log.d("BlogDataTypeUtil", "downloadData start")

        val snapshotDataDirectory = DataCacheFolderUtil.createEmptyFolder(artifactId, date)

        // todo make new webview if the current one isn't prepared, and load content using it
        //val urlToDownload = dataTypeSpecificMetadata.urls[0]
        //webViewWithContent.settings.javaScriptEnabled = true
        //webViewWithContent.settings.allowFileAccess = true
        //webViewWithContent.loadUrl(urlToDownload)

        val webArchivePath = snapshotDataDirectory + File.separator + "index.mht"

        // Async "nature" of saveWebArchive propagated to caller methods as "sync" method
        // using suspend coroutine of Kotlin
        suspendCoroutine<String> { continuation ->
            webViewWithContent.saveWebArchive(webArchivePath, false) {
                Log.d("BlogDataTypeUtil", "saveWebArchive ended, resuming")
                continuation.resume(webArchivePath)
            }
            Log.d("BlogDataTypeUtil", "saveWebArchive started, suspending")
        }
        Log.d("BlogDataTypeUtil", "downloadData end")
        return snapshotDataDirectory
    }

}