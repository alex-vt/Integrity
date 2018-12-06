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
import kotlinx.coroutines.delay
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
        val snapshotDataDirectory = DataCacheFolderUtil.createSnapshotFolder(artifactId, date)

        try {
            if (!blogMetadata.paginationUsed) {
                collectLinksAtPage(
                        webView = webView,
                        blogMetadata = blogMetadata,
                        snapshotDataDirectory = snapshotDataDirectory,
                        pageUrl = blogMetadata.url,
                        pageFullIndex = "",
                        loadIntervalMillis = blogMetadata.loadIntervalMillis,
                        jobProgressListener = jobProgressListener,
                        jobCoroutineContext = jobCoroutineContext
                )
            } else {
                for (pageIndex in blogMetadata.pagination.startIndex..blogMetadata.pagination.limit
                        step blogMetadata.pagination.step) {
                    collectLinksAtPage(
                            webView = webView,
                            blogMetadata = blogMetadata,
                            snapshotDataDirectory = snapshotDataDirectory,
                            pageUrl = blogMetadata.url + blogMetadata.pagination.path + pageIndex,
                            pageFullIndex = " $pageIndex of ${blogMetadata.pagination.limit}",
                            loadIntervalMillis = blogMetadata.loadIntervalMillis,
                            jobProgressListener = jobProgressListener,
                            jobCoroutineContext = jobCoroutineContext
                    )
                }
            }
            val urlsToDownload = getLinksFromFile(snapshotDataDirectory)
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

    private suspend fun collectLinksAtPage(webView: WebView, blogMetadata: BlogTypeMetadata,
                                           snapshotDataDirectory: String, pageUrl: String,
                                           pageFullIndex: String, loadIntervalMillis: Long,
                                           jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
                                           jobCoroutineContext: CoroutineContext) {
        if (!jobCoroutineContext.isActive) {
            return
        }
        if (pageAlreadyProcessed(pageUrl, snapshotDataDirectory)) {
            return
        }
        jobProgressListener.invoke(JobProgress(
                progressMessage = "Collecting links for page$pageFullIndex:\n$pageUrl"
        ))
        addLinksToFile(getPageLinks(
                webView, pageUrl, loadIntervalMillis,
                blogMetadata.relatedPageLinksUsed,
                blogMetadata.relatedPageLinksPattern,
                blogMetadata.loadImages, blogMetadata.desktopSite
        ), snapshotDataDirectory)
        addPageToFile(pageUrl, snapshotDataDirectory)
    }

    private fun pageAlreadyProcessed(pageUrl: String, snapshotDataDirectory: String)
            = DataCacheFolderUtil.readTextFromFile("$snapshotDataDirectory/pagination.txt")
            .contains(pageUrl + "\n") // while link in a line

    private fun addPageToFile(pageLink: String, snapshotDataDirectory: String)
            = DataCacheFolderUtil.addTextToFile(pageLink,
            "$snapshotDataDirectory/pagination.txt")


    private fun addLinksToFile(links: Collection<String>, snapshotDataDirectory: String)
            = DataCacheFolderUtil.addTextToFile(links.joinToString("\n"),
                "$snapshotDataDirectory/links.txt")

    private fun getLinksFromFile(snapshotDataDirectory: String): Set<String>
            = DataCacheFolderUtil.readTextFromFile("$snapshotDataDirectory/links.txt")
            .split("\n").filter { it.isNotBlank() }.toSet()

    /**
     * Gets links from page based on its pageUrl,
     * and firstPageUrl to match links with (must contain it),
     * according to cssSelector.
     */
    private suspend fun getPageLinks(webView: WebView, pageUrl: String, loadIntervalMillis: Long,
                                     selectRelatedLinks: Boolean, cssSelector: String,
                                     loadImages: Boolean, desktopSite: Boolean): Set<String> {
        val relatedPageUrls = linkedSetOf(pageUrl)
        if (selectRelatedLinks) {
            val pageHtml = WebViewUtil.loadHtml(webView, pageUrl, loadImages, desktopSite, setOf())
            delay(loadIntervalMillis)
            val selectedLinkMap = LinkUtil.getCssSelectedLinkMap(pageHtml, cssSelector, pageUrl)
            Log.d("BlogDataTypeUtil", "getPageLinks: selectRelatedLinks = ${selectedLinkMap.keys}")
            relatedPageUrls.addAll(selectedLinkMap.keys)
        }
        Log.d("BlogDataTypeUtil", "getPageLinks: Obtained ${relatedPageUrls.size} links" +
                " (related links selected: $selectRelatedLinks)" +
                " by pattern $cssSelector \nat page $pageUrl")
        return relatedPageUrls
    }
}