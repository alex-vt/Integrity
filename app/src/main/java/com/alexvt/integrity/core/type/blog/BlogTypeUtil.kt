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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
            val webView = WebView(IntegrityCore.context)

            while (isRunning(jobContext) && hasNextPaginationLink(blogMetadata, snapshotPath)) {
                val pageLink = getNextPaginationLink(blogMetadata, snapshotPath)
                val relatedPageLinks = getPageLinks(
                        webView,
                        pageLink,
                        blogMetadata.loadIntervalMillis,
                        blogMetadata.relatedPageLinksUsed,
                        blogMetadata.relatedPageLinksPattern,
                        blogMetadata.loadImages,
                        blogMetadata.desktopSite,
                        getPaginationProgressText(blogMetadata, snapshotPath),
                        jobProgressListener
                )
                WebViewUtil.saveArchives(
                        webView,
                        relatedPageLinks,
                        snapshotPath,
                        isFirstPage(snapshotPath),
                        getPaginationProgressText(blogMetadata, snapshotPath),
                        blogMetadata.loadIntervalMillis,
                        blogMetadata.loadImages,
                        blogMetadata.desktopSite,
                        jobProgressListener,
                        jobContext
                )
                saveLinks(pageLink, relatedPageLinks, snapshotPath)
            }
        }

        Log.d("BlogDataTypeUtil", "downloadData end")
        return snapshotPath
    }


    private fun isRunning(jobContext: CoroutineContext) = jobContext.isActive

    private fun hasNextPaginationLink(blogMetadata: BlogTypeMetadata, snapshotPath: String)
            = BlogTypeHelper.getNextPaginationLinks(blogMetadata, snapshotPath)
            .isNotEmpty()

    private fun getNextPaginationLink(blogMetadata: BlogTypeMetadata, snapshotPath: String)
            = BlogTypeHelper.getNextPaginationLinks(blogMetadata, snapshotPath)
            .first()

    private fun getPaginationProgressText(blogMetadata: BlogTypeMetadata,
                                          snapshotPath: String): String {
        val forPageText = "for page: ${getNextPaginationLink(blogMetadata, snapshotPath)}"
        val index = BlogTypeHelper.getPreviousPaginationLinks(snapshotPath).size + 1
        val count = BlogTypeHelper.getAllKnownPaginationLinks(blogMetadata).size
        if (count > 1) {
            return "$forPageText ($index of $count)"
        }
        return forPageText
    }

    private fun isFirstPage(snapshotPath: String)
            = BlogTypeHelper.getPreviousPaginationLinks(snapshotPath).isEmpty()

    private fun saveLinks(paginationLink: String, relatedPageLinks: Collection<String>,
                          snapshotPath: String) {
        BlogTypeHelper.saveRelatedPageLinks(relatedPageLinks, snapshotPath)
        BlogTypeHelper.savePaginationLink(paginationLink, snapshotPath)
    }


    /**
     * Gets links from page based on its pageUrl,
     * and firstPageUrl to match links with (must contain it),
     * according to cssSelector.
     */
    private suspend fun getPageLinks(webView: WebView, pageUrl: String, loadIntervalMillis: Long,
                                     selectRelatedLinks: Boolean, cssSelector: String,
                                     loadImages: Boolean, desktopSite: Boolean,
                                     pageProgressText: String,
                                     jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit
    ): Set<String> {
        IntegrityCore.postProgress(jobProgressListener, "Collecting links\n$pageProgressText")
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