/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.webkit.ValueCallback
import com.alexvt.integrity.R.id.webView



/**
 * Performs main operations with web pages in the provided WebView.
 */
object WebViewUtil {

    lateinit var htmlLoadingContinuation: Continuation<String>

    @JavascriptInterface
    fun onWebPageHtmlLoaded(html: String) {
        Log.d("WebViewUtil", "onWebPageHtmlLoaded: HTML length ${html.length}")
        WebViewUtil.htmlLoadingContinuation.resume(html)
    }

    suspend fun loadHtml(webView: WebView, url: String, localLinkHashes: Collection<String>): String
            = suspendCoroutine { continuation ->
        htmlLoadingContinuation = continuation

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
            var jsLoaded = false // for dealing with possible multiple progress == 100
            override fun onProgressChanged(view: WebView, progress: Int) {
                Log.d("WebViewUtil", "Loading " + progress + "%: " + view.url)
                if (progress == 100) {
                    if (view.url.startsWith("http")) {
                        if (jsLoaded) {
                            return
                        }
                        jsLoaded = true
                        webView.loadUrl("javascript:window.jsi.onWebPageHtmlLoaded('<head>'" +
                                "+document.getElementsByTagName('html')[0].innerHTML+'</head>');")
                    } else if (view.url.startsWith("file:")) {
                        if (jsLoaded) {
                            return
                        }
                        jsLoaded = true
                        // todo fix Blocked script execution in 'file:///....mht' because the
                        // document's frame is sandboxed and the 'allow-scripts' permission is not set.
                        WebViewUtil.htmlLoadingContinuation.resume("")
                    }
                }
            }
        }
        webView.loadUrl(url)
    }

    suspend fun saveArchives(webView: WebView, urlToArchivePathMap: Map<String, String>) {
        urlToArchivePathMap.entries.forEachIndexed { index, entry -> run {
            Log.d("WebViewUtil", "saveArchives: saving " + (index + 1) + " of "
                    + urlToArchivePathMap.size)
            saveArchive(webView, entry.key, entry.value)
            Log.d("WebViewUtil", "saveArchives: saved " + (index + 1) + " of "
                    + urlToArchivePathMap.size)
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
