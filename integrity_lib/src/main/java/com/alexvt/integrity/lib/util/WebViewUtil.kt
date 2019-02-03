/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.util

import android.content.Context
import android.content.res.Resources
import android.webkit.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.webkit.ConsoleMessage
import kotlinx.coroutines.*
import android.webkit.WebSettings
import android.widget.Toast
import com.alexvt.integrity.lib.Log
import java.util.*
import kotlin.concurrent.schedule
import android.graphics.Bitmap
import android.graphics.Canvas




/**
 * Performs main operations with web pages in the provided WebView.
 */
object WebViewUtil {

    lateinit var pageLoadListener: (String) -> Unit

    @JavascriptInterface
    fun onWebPageHtmlLoaded(html: String) {
        android.util.Log.v("WebViewUtil", "onWebPageHtmlLoaded: HTML length ${html.length}")
        // listener shouldn't be invoked in JavaScript execution thread
        GlobalScope.launch (Dispatchers.Main) {
            pageLoadListener.invoke(html)
        }
    }

    suspend fun loadHtml(webView: WebView, url: String, loadImages: Boolean, desktopSite: Boolean): String
            = suspendCoroutine { continuation ->
        loadHtml(webView, url, emptyMap(), loadImages, desktopSite) {
            try {
                continuation.resume(it)
            } catch (t: Throwable) {
                // todo support job cancellation
                Log(webView.context, "Html loading error").logError(t)
            }
        }
    }

    fun loadHtml(webView: WebView, startUrl: String, urlRedirectMap: Map<String, String>,
                 loadImages: Boolean, desktopSite: Boolean,
                 pageLoadListener: (String) -> Unit) {
        WebViewUtil.pageLoadListener = pageLoadListener
        setupWebView(webView, isOfflineLoading(startUrl), loadImages, desktopSite)

        webView.addJavascriptInterface(this, "jsi") // registers onWebPageHtmlLoaded

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // links from locally loaded HTML can be redirected to corresponding locally saved pages
                if (isUrlRedirected(urlRedirectMap, url)) {
                    webView.loadUrl(getRedirectUrl(urlRedirectMap, url))
                } else {
                    if (isOfflineLoading(startUrl)) {
                        android.util.Log.v("WebViewUtil", "Page $url isn't archived.")
                        Toast.makeText(webView.context, "Page $url isn't archived.",
                                Toast.LENGTH_SHORT).show()
                        return true
                    } else {
                        webView.loadUrl(url)
                    }
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            var previousProgress = 0 // for dealing with possible multiple progress == 100

            override fun onProgressChanged(view: WebView, progress: Int) {
                android.util.Log.v("WebViewUtil", "Loading " + progress + "%: " + view.url)
                if (progress == 100 && previousProgress != 100) {
                    android.util.Log.v("WebViewUtil", "Loading complete: ${view.url}")
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
                android.util.Log.v("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber())
                return true
            }
        }
        webView.loadUrl(startUrl)
    }

    private fun isOfflineLoading(firstPageUrl: String) = firstPageUrl.startsWith("file:")

    private fun isUrlRedirected(urlRedirectMap: Map<String, String>, url: String)
            = urlRedirectMap.containsKey(url.trimEnd('/'))

    private fun getRedirectUrl(urlRedirectMap: Map<String, String>, url: String)
            = urlRedirectMap[url.trimEnd('/')]

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

    private fun setDesktopMode(webView: WebView, enabled: Boolean) {
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

    // Loads the web page and gets its screenshot.
    fun getScreenshot(context: Context, url: String, loadIntervalMillis: Long,
                      loadImages: Boolean, desktopSite: Boolean): Bitmap {
        lateinit var screenshotBitmap: Bitmap
        runBlocking(Dispatchers.Main) {
            screenshotBitmap = getScreenshotInternal(WebView(context), url, loadIntervalMillis,
                    loadImages, desktopSite)
        }
        return screenshotBitmap
    }

    // Loads the web page and gets its screenshot,
    // as "sync" method using suspend coroutine of Kotlin
    private suspend fun getScreenshotInternal(webView: WebView, url: String,
                                              loadIntervalMillis: Long,
                                              loadImages: Boolean, desktopSite: Boolean)
            = suspendCoroutine<Bitmap> { continuation ->
        setupWebView(webView = webView, loadingOffline = isOfflineLoading(url),
                loadImages = loadImages, desktopSite = desktopSite)

        val screenshotSize = getScreenshotSize()
        webView.measure(screenshotSize, screenshotSize)
        webView.layout(0, 0, screenshotSize, screenshotSize)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, urlNewString: String): Boolean {
                webView.loadUrl(urlNewString)
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            var previousProgress = 0 // for dealing with possible multiple progress == 100

            override fun onProgressChanged(view: WebView, progress: Int) {
                android.util.Log.v("WebViewUtil", "Loading " + progress + "%: " + view.url)
                resetLoadingTimeoutTimer(webView)
                if (progress == 100 && previousProgress != 100) {
                    webView.stopLoading()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(loadIntervalMillis)
                        cancelLoadingTimeoutTimer()
                        try {
                            continuation.resume(getScreenshot(webView, screenshotSize))
                            android.util.Log.v("WebViewUtil", "getScreenshot captured screenshot")
                        } catch (t: Throwable) {
                            android.util.Log.v("WebViewUtil", "getScreenshot: already resumed")
                        }
                    }
                }
                previousProgress = progress
            }

            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                // this is verbose
                android.util.Log.v("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber())
                return true
            }
        }
        webView.loadUrl(url)
        android.util.Log.v("WebViewUtil", "getScreenshot: loading $url")
    }

    private const val screenshotDefaultSize = 1280

    private fun getScreenshotSize() = Math.max(screenshotDefaultSize,
            Math.min(Resources.getSystem().displayMetrics.widthPixels,
                    Resources.getSystem().displayMetrics.heightPixels))

    private fun getScreenshot(webView: WebView, screenshotSize: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(screenshotSize, screenshotSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        return bitmap
    }

    // Async "nature" of saveWebArchive propagated to caller methods as "sync" method
    // using suspend coroutine of Kotlin
    suspend fun saveArchive(webView: WebView, url: String, webArchivePath: String,
                                    loadIntervalMillis: Long, loadImages: Boolean,
                                    desktopSite: Boolean) = suspendCoroutine<String> { continuation ->
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
                android.util.Log.v("WebViewUtil", "Loading " + progress + "%: " + view.url)
                resetLoadingTimeoutTimer(webView)
                if (progress == 100 && previousProgress != 100) {
                    webView.stopLoading()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(loadIntervalMillis)
                        // always have some delay to prevent saveWebArchive from being stuck, todo investigate
                        maintainProcessPriority()
                        webView.saveWebArchive(webArchivePath, false) {
                            android.util.Log.v("WebViewUtil", "saveWebArchive ended")
                            cancelLoadingTimeoutTimer()
                            try {
                                continuation.resume(webArchivePath)
                            } catch (t: Throwable) {
                                android.util.Log.v("WebViewUtil", "saveWebArchive: already resumed")
                            }
                        }
                        android.util.Log.v("WebViewUtil", "saveWebArchive started")
                    }
                }
                previousProgress = progress
            }

            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                // this is verbose
                android.util.Log.v("TAG", cm.message() + " at " + cm.sourceId() + ":" + cm.lineNumber())
                return true
            }
        }
        webView.loadUrl(url)
        android.util.Log.v("WebViewUtil", "saveArchive: loading $url")
    }


    private fun maintainProcessPriority() {
        val worstTargetProcessPriority = android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE
        val processId = android.os.Process.myTid()
        val originalPriority = android.os.Process.getThreadPriority(processId)
        if (originalPriority > worstTargetProcessPriority) {
            android.os.Process.setThreadPriority(worstTargetProcessPriority)
            android.util.Log.v("WebViewUtil", "maintaining thread priority: " +
                    "$originalPriority -> ${android.os.Process.getThreadPriority(processId)}")
        } else {
            android.util.Log.v("WebViewUtil", "maintaining thread priority: " +
                    "$originalPriority")
        }
    }

    private var loadingTimeoutTimer: TimerTask = Timer().schedule(0) {}

    private fun resetLoadingTimeoutTimer(webView: WebView) {
        cancelLoadingTimeoutTimer()
        val loadingTimeoutMillis = 15000L
        loadingTimeoutTimer = Timer().schedule(loadingTimeoutMillis) {
                    GlobalScope.launch(Dispatchers.Main) {
                        Log(webView.context,"Reloading on timeout $loadingTimeoutMillis ms: ${webView.url}")
                                .logError()
                        webView.reload()
                    }
                }
    }

    private fun cancelLoadingTimeoutTimer() {
        loadingTimeoutTimer.cancel()
    }
}
