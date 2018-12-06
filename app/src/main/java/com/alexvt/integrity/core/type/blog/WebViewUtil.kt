/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import android.webkit.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.job.JobProgress
import android.webkit.ConsoleMessage
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import android.webkit.WebSettings



/**
 * Performs main operations with web pages in the provided WebView.
 */
object WebViewUtil {

    lateinit var pageLoadListener: (String) -> Unit

    @JavascriptInterface
    fun onWebPageHtmlLoaded(html: String) {
        Log.d("WebViewUtil", "onWebPageHtmlLoaded: HTML length ${html.length}")
        // listener shouldn't be invoked in JavaScript execution thread
        GlobalScope.launch (Dispatchers.Main) {
            WebViewUtil.pageLoadListener.invoke(html)
        }
    }

    suspend fun loadHtml(webView: WebView, url: String, loadImages: Boolean, desktopSite: Boolean,
                         localLinkHashes: Collection<String>): String
            = suspendCoroutine { continuation ->
        loadHtml(webView, url, localLinkHashes, loadImages, desktopSite) {
            try {
                continuation.resume(it)
            } catch (t: Throwable) {
                Log.e("WebViewUtil", "loadHtml continuation failed") // todo support job cancellation
            }
        }
    }

    fun loadHtml(webView: WebView, startUrl: String, localLinkHashes: Collection<String>,
                 loadImages: Boolean, desktopSite: Boolean,
                 pageLoadListener: (String) -> Unit) {
        WebViewUtil.pageLoadListener = pageLoadListener
        setupWebView(webView, isOfflineLoading(startUrl), loadImages, desktopSite)

        webView.addJavascriptInterface(this, "jsi") // registers onWebPageHtmlLoaded

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, newUrl: String): Boolean {
                // links from locally loaded HTML can be redirected to corresponding locally saved pages
                if (isOfflineLoading(startUrl) && isLocallySavedLinkHit(localLinkHashes, newUrl)) {
                    webView.loadUrl(getLocallySavedPageUrl(startUrl, newUrl))
                } else {
                    if (isOfflineLoading(startUrl)) {
                        Log.w("WebViewUtil", "Offline archives don't store page: $newUrl " +
                                "(hash ${newUrl.hashCode()})")
                    }
                    webView.loadUrl(newUrl)
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            var previousProgress = 0 // for dealing with possible multiple progress == 100
            override fun onProgressChanged(view: WebView, progress: Int) {
                Log.d("WebViewUtil", "Loading " + progress + "%: " + view.url)
                if (progress == 100 && previousProgress != 100) {
                    Log.d("WebViewUtil", "Loading complete: ${view.url}")
                    if (view.url.startsWith("http")) {
                        webView.loadUrl("javascript:window.jsi.onWebPageHtmlLoaded('<head>'" +
                                "+document.getElementsByTagName('html')[0].innerHTML+'</head>');")
                        // will resume in onWebPageHtmlLoaded

                    } else if (view.url.startsWith("file:")) {
                        // todo fix Blocked script execution in 'file:///....mht' because the
                        // document's frame is sandboxed and the 'allow-scripts' permission is not set.
                        WebViewUtil.pageLoadListener.invoke("")
                    }
                }
                previousProgress = progress
            }

            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                // this is verbose
                Log.v("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber())
                return true
            }
        }
        webView.loadUrl(startUrl)
    }

    private fun isOfflineLoading(firstPageUrl: String) = firstPageUrl.startsWith("file:")

    /**
     * Determines if a page with given URL is stored locally.
     *
     * Locally stored web archives have names
     * that contain the corresponding web page link hash codes.
      */
    private fun isLocallySavedLinkHit(localLinkHashes: Collection<String>, url: String)
            = localLinkHashes.contains(url.hashCode().toString())

    /**
     * Returns a redirect link to load page from file instead of loading by its web URL.
     *
     * Links from locally loaded HTML can be redirected to corresponding local pages
     * named page_<linkHash>.mht, similarly to first page name index.mht.
     */
    private fun getLocallySavedPageUrl(firstPageLocalUrl: String, newPageWebUrl: String)
            = firstPageLocalUrl.replace("index.mht", "page_${newPageWebUrl.hashCode()}.mht")

    private fun setupWebView(webView: WebView, loadingOffline: Boolean, loadImages: Boolean,
                             desktopSite: Boolean) {
        webView.settings.allowFileAccess = true
        webView.settings.javaScriptEnabled = true
        webView.settings.saveFormData = false
        webView.settings.loadsImagesAutomatically = loadImages
        webView.settings.setAppCacheEnabled(true)
        webView.settings.setAppCachePath(webView.context.cacheDir.absolutePath)
        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.clearHistory()
        webView.clearCache(true)
        webView.settings.cacheMode = if (loadingOffline) {
            WebSettings.LOAD_CACHE_ONLY
        } else {
            WebSettings.LOAD_NO_CACHE
        }
        setDesktopMode(webView, desktopSite)
    }

    fun setDesktopMode(webView: WebView, enabled: Boolean) {
        // see https://github.com/delight-im/Android-AdvancedWebView/blob/master/Source/library/src/main/java/im/delight/android/webview/AdvancedWebView.java
        val newUserAgent = if (enabled) {
            webView.settings.userAgentString.replace("Mobile", "eliboM").replace("Android", "diordnA")
        } else {
            webView.settings.userAgentString.replace("eliboM", "Mobile").replace("diordnA", "Android")
        }
        webView.settings.userAgentString = newUserAgent
        webView.settings.useWideViewPort = enabled
        webView.settings.loadWithOverviewMode = enabled
        webView.settings.setSupportZoom(enabled)
        webView.settings.builtInZoomControls = enabled
    }

    suspend fun saveArchives(webView: WebView, urlToArchivePathMap: Map<String, String>,
                             loadIntervalMillis: Long, loadImages: Boolean, desktopSite: Boolean,
                             jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
                             jobCoroutineContext: CoroutineContext) {
        urlToArchivePathMap.entries.forEachIndexed { index, entry -> run {
            if (!jobCoroutineContext.isActive) {
                return
            }
            Log.d("WebViewUtil", "saveArchives: saving " )
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Saving web archive " + (index + 1) + " of "
                            + urlToArchivePathMap.size
            ))
            saveArchive(webView, entry.key, entry.value, loadIntervalMillis, loadImages, desktopSite)
        } }
    }

    // Async "nature" of saveWebArchive propagated to caller methods as "sync" method
    // using suspend coroutine of Kotlin
    private suspend fun saveArchive(webView: WebView, url: String, webArchivePath: String,
                                    loadIntervalMillis: Long, loadImages: Boolean,
                                    desktopSite: Boolean)
            = suspendCoroutine<String> { continuation ->
        setupWebView(webView = webView, loadingOffline = false, loadImages = loadImages,
                desktopSite = desktopSite)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, urlNewString: String): Boolean {
                webView.loadUrl(urlNewString)
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            var previousProgress = 0 // for dealing with possible multiple progress == 100

            override fun onProgressChanged(view: WebView, progress: Int) {
                Log.d("WebViewUtil", "Loading " + progress + "%: " + view.url)
                if (progress == 100 && previousProgress != 100) {
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(loadIntervalMillis)
                        webView.saveWebArchive(webArchivePath, false) {
                            Log.d("WebViewUtil", "saveWebArchive ended")
                            continuation.resume(webArchivePath)
                        }
                    }
                    Log.d("WebViewUtil", "saveWebArchive started")
                }
                previousProgress = progress
            }

            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                // this is verbose
                Log.v("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber())
                return true
            }
        }
        webView.loadUrl(url)
        Log.d("WebViewUtil", "saveArchive: loading $url")
    }
}
