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
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext


class BlogTypeUtil: DataTypeUtil<BlogTypeMetadata> {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getOperationMainActivityClass() = BlogTypeActivity::class.java

    override suspend fun downloadData(artifactId: Long, date: String,
                                      blogMetadata: BlogTypeMetadata,
                                      jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
                                      jobCoroutineContext: CoroutineContext): String {
        Log.d("BlogDataTypeUtil", "downloadData start")

        val webView = WebView(IntegrityCore.context)
        val snapshotDataDirectory = DataCacheFolderUtil.createEmptyFolder(artifactId, date)
        val urlsToDownload = LinkedHashSet<String>()

        try {
            if (!blogMetadata.paginationUsed) {
                jobProgressListener.invoke(JobProgress(
                        progressMessage = "Collecting links for page:\n${blogMetadata.url}"
                ))
                urlsToDownload.addAll(getPageLinks(
                        webView, blogMetadata.url, blogMetadata.url,
                        blogMetadata.relatedPageLinksUsed,
                        blogMetadata.relatedPageLinksPattern,
                        blogMetadata.loadImages, blogMetadata.desktopSite
                ))
            } else {
                for (pageIndex in blogMetadata.pagination.startIndex..blogMetadata.pagination.limit
                        step blogMetadata.pagination.step) {
                    if (!jobCoroutineContext.isActive) {
                        break
                    }
                    val pageUrl = blogMetadata.url + blogMetadata.pagination.path + pageIndex
                    jobProgressListener.invoke(JobProgress(
                            progressMessage = "Collecting links for page $pageIndex " +
                                    "of ${blogMetadata.pagination.limit}:\n$pageUrl"
                    ))
                    urlsToDownload.addAll(getPageLinks(
                            webView, blogMetadata.url, pageUrl,
                            blogMetadata.relatedPageLinksUsed,
                            blogMetadata.relatedPageLinksPattern,
                            blogMetadata.loadImages, blogMetadata.desktopSite
                    ))
                }
            }

            // first page should be saved as index.mht
            val allTargetUrlToArchivePathMap = urlsToDownload.take(1)
                    .associate { it to "$snapshotDataDirectory/index.mht" }
                    .plus(urlsToDownload.drop(1)
                            .associate { it to "$snapshotDataDirectory/page_${it.hashCode()}.mht"})

            WebViewUtil.saveArchives(webView, allTargetUrlToArchivePathMap,
                    blogMetadata.loadIntervalMillis, blogMetadata.loadImages,
                    blogMetadata.desktopSite, jobProgressListener, jobCoroutineContext)

            Log.d("BlogDataTypeUtil", "downloadData end")
        } catch (t: Throwable) {
            Log.e("BlogDataTypeUtil", "downloadData exception", t)
        }
        return snapshotDataDirectory
    }

    /**
     * Gets links from page based on its pageUrl,
     * and firstPageUrl to match links with (must contain it),
     * according to cssSelector.
     */
    private suspend fun getPageLinks(webView: WebView, firstPageUrl: String, pageUrl: String,
                                     selectRelatedLinks: Boolean, cssSelector: String,
                                     loadImages: Boolean, desktopSite: Boolean): Set<String> {
        val pageHtml = WebViewUtil.loadHtml(webView, pageUrl, loadImages, desktopSite, setOf())
        val relatedPageUrls = linkedSetOf(pageUrl)
        if (selectRelatedLinks) {
            val selectedLinkMap = LinkUtil.getCssSelectedLinkMap(pageHtml, cssSelector, firstPageUrl)
            Log.d("BlogDataTypeUtil", "getPageLinks: selectRelatedLinks = ${selectedLinkMap}")
            relatedPageUrls.addAll(selectedLinkMap.keys)
        }
        Log.d("BlogDataTypeUtil", "getPageLinks: Obtained ${relatedPageUrls.size} links" +
                " (related links selected: $selectRelatedLinks)" +
                " by pattern $cssSelector \nat page $pageUrl")
        return relatedPageUrls
    }

}