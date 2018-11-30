/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import android.webkit.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.job.JobProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


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

    suspend fun loadHtml(webView: WebView, url: String, localLinkHashes: Collection<String>): String
            = suspendCoroutine { continuation ->
        loadHtml(webView, url, localLinkHashes) {
            continuation.resume(it)
        }
    }

    fun loadHtml(webView: WebView, url: String, localLinkHashes: Collection<String>,
                         pageLoadListener: (String) -> Unit) {
        WebViewUtil.pageLoadListener = pageLoadListener

        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.saveFormData = false
        webView.settings.loadsImagesAutomatically = true
        webView.settings.setAppCacheEnabled(false)
        webView.clearHistory()

        webView.addJavascriptInterface(this, "jsi") // registers onWebPageHtmlLoaded

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, urlNewString: String): Boolean {
                // links from locally loaded HTML can point to local pages named page_<linkHash>
                if (url.startsWith("file:") && localLinkHashes.contains("" + urlNewString.hashCode())) {
                    webView.loadUrl(url.replace("index.mht",
                            "page_" + urlNewString.hashCode() + ".mht"))
                } else {
                    webView.loadUrl(urlNewString)
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
        }
        webView.loadUrl(url)
    }

    suspend fun saveArchives(webView: WebView, urlToArchivePathMap: Map<String, String>,
                             jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit) {
        urlToArchivePathMap.entries.forEachIndexed { index, entry -> run {
            Log.d("WebViewUtil", "saveArchives: saving " )
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Saving web archive " + (index + 1) + " of "
                            + urlToArchivePathMap.size
            ))
            saveArchive(webView, entry.key, entry.value)
        } }
    }

    // Async "nature" of saveWebArchive propagated to caller methods as "sync" method
    // using suspend coroutine of Kotlin
    private suspend fun saveArchive(webView: WebView, url: String, webArchivePath: String)
            = suspendCoroutine<String> { continuation ->
        webView.settings.allowFileAccess = true
        webView.settings.javaScriptEnabled = true
        webView.settings.saveFormData = false
        webView.settings.loadsImagesAutomatically = true
        webView.settings.setAppCacheEnabled(false)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, urlNewString: String): Boolean {
                webView.loadUrl(urlNewString)
                return false
            }
        }

        var alreadyLoaded = false // for preventing triggering events on load twice
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (alreadyLoaded) {
                    return
                }
                Log.d("WebViewUtil", "Loading " + progress + "%: " + view.url)
                if (progress == 100) {
                    alreadyLoaded = true
                    webView.saveWebArchive(webArchivePath, false) {
                        Log.d("WebViewUtil", "saveWebArchive ended")
                        continuation.resume(webArchivePath)
                    }
                    Log.d("WebViewUtil", "saveWebArchive started")
                }
            }
        }
        webView.loadUrl(url)
        Log.d("WebViewUtil", "saveArchive: loading $url")
    }
}
