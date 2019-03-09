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
import com.alexvt.integrity.lib.log.Log
import java.util.*
import kotlin.concurrent.schedule
import android.graphics.Bitmap
import android.graphics.Canvas
import com.alexvt.integrity.lib.filesystem.AndroidFilesystemManager
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import java.io.ByteArrayOutputStream

/**
 * Performs main operations with web pages in the provided WebView.
 */
class WebPageLoader {

    private lateinit var pageLoadListener: (String) -> Unit

    @JavascriptInterface
    fun onWebPageHtmlLoaded(html: String) {
        android.util.Log.v("WebPageLoader", "onWebPageHtmlLoaded (loader ${this}): " +
                "HTML length ${html.length}")
        // listener shouldn't be invoked in JavaScript execution thread
        GlobalScope.launch (Dispatchers.Main) {
            pageLoadListener.invoke(html)
        }
    }

    /**
     * Async requests web page to be loaded into a given WebView in UI thread.
     * Invokes the listener with the web page HTML when loaded.
     */
    fun loadHtml(webView: WebView, startUrl: String, urlRedirectMap: Map<String, String>,
                 loadImages: Boolean, desktopSite: Boolean, pageLoadListener: (String) -> Unit) {
        loadPageInternal(webView, startUrl, urlRedirectMap, loadImages, desktopSite,
                null, 0, null, pageLoadListener) // just HTML loading
    }

    /**
     * Loads web page in any thread.
     * Returns the web page HTML when loaded.
     */
    fun getHtml(context: Context, url: String, loadImages: Boolean,
                desktopSite: Boolean, archiveSavePath: String?,
                screenshotSavePath: String?,
                delayMillis: Long): String {
        return getPageInternal(getWebView(context), url, emptyMap(), loadImages, desktopSite,
                archiveSavePath, delayMillis, screenshotSavePath)
    }

    private fun getWebView(context: Context): WebView {
        var webView: WebView? = null
        runBlocking(Dispatchers.Main) {
            webView = WebView(context)
        }
        return webView!!
    }

    private fun getPageInternal(webView: WebView, startUrl: String,
                                urlRedirectMap: Map<String, String>,
                                loadImages: Boolean, desktopSite: Boolean,
                                archiveSavePath: String?, delayMillis: Long,
                                screenshotSavePath: String?): String {
        var html = ""
        runBlocking(Dispatchers.Main) { // todo unload main thread
            html = suspendCoroutine { continuation ->
                loadPageInternal(webView, startUrl, urlRedirectMap, loadImages,
                        desktopSite, archiveSavePath, delayMillis, screenshotSavePath) {
                    continuation.resume(it)
                }
            }
        }
        return html
    }

    private fun loadPageInternal(webView: WebView, startUrl: String,
                                 urlRedirectMap: Map<String, String>,
                                 loadImages: Boolean, desktopSite: Boolean,
                                 archiveSavePath: String?, delayMillis: Long,
                                 screenshotSavePath: String?,
                                 pageLoadListener: (String) -> Unit) {
        this@WebPageLoader.pageLoadListener = pageLoadListener
        setupWebView(webView, isOfflineLoading(startUrl), loadImages, desktopSite)

        val screenshotSize = getScreenshotSize()
        if (screenshotSavePath != null) {
            webView.measure(screenshotSize, screenshotSize)
            webView.layout(0, 0, screenshotSize, screenshotSize)
        }

        webView.addJavascriptInterface(this, "jsi") // registers onWebPageHtmlLoaded

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // links from locally loaded HTML can be redirected to corresponding locally saved pages
                if (isUrlRedirected(urlRedirectMap, url)) {
                    webView.loadUrl(getRedirectUrl(urlRedirectMap, url))
                } else {
                    if (isOfflineLoading(startUrl)) {
                        android.util.Log.v("WebPageLoader", "Page $url isn't archived.")
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
                android.util.Log.v("WebPageLoader", "Loading " + progress + "%: " + view.url)
                // todo watch for timeout
                if (progress == 100 && previousProgress != 100) {
                    android.util.Log.v("WebPageLoader", "Loading complete: ${view.url}")
                    if (view.url.startsWith("http")) {
                        if (archiveSavePath != null) {
                            webView.stopLoading()
                            GlobalScope.launch(Dispatchers.Main) {
                                delay(delayMillis)
                                // always have some delay to prevent saveWebArchive from being stuck, todo investigate
                                maintainProcessPriority()
                                webView.saveWebArchive(archiveSavePath, false) {
                                    android.util.Log.v("WebPageLoader", "saveWebArchive ended")
                                    requestPageContent(webView)
                                }
                                android.util.Log.v("WebPageLoader", "saveWebArchive started")
                            }
                        } else {
                            requestPageContent(webView)
                        }

                    } else if (view.url.startsWith("file:")) {
                        // todo fix Blocked script execution in 'file:///....mht' because the
                        // document's frame is sandboxed and the 'allow-scripts' permission is not set.
                        if (screenshotSavePath != null) {
                            webView.stopLoading()
                            GlobalScope.launch(Dispatchers.Main) {
                                delay(delayMillis)
                                val previewBitmap = getPageBitmap(webView, screenshotSize)
                                DataFolderManager(AndroidFilesystemManager(view.context))
                                        .writeFile(getBytes(previewBitmap), screenshotSavePath)
                                this@WebPageLoader.pageLoadListener.invoke("")
                                android.util.Log.v("WebPageLoader", "captured screenshot")
                            }
                        } else {
                            this@WebPageLoader.pageLoadListener.invoke("")
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
        webView.loadUrl(startUrl)
    }

    private fun getBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun requestPageContent(webView: WebView) {
        android.util.Log.v("WebPageLoader", "Extracting page content using JS: ${webView.url}")
        webView.loadUrl("javascript:window.jsi.onWebPageHtmlLoaded('<head>'" +
                "+document.getElementsByTagName('html')[0].innerHTML+'</head>');")
        // will resume in onWebPageHtmlLoaded
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


    private val screenshotDefaultSize = 1280

    private fun getScreenshotSize() = Math.max(screenshotDefaultSize,
            Math.min(Resources.getSystem().displayMetrics.widthPixels,
                    Resources.getSystem().displayMetrics.heightPixels))

    private fun getPageBitmap(webView: WebView, screenshotSize: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(screenshotSize, screenshotSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        return bitmap
    }


    private fun maintainProcessPriority() {
        val worstTargetProcessPriority = android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE
        val processId = android.os.Process.myTid()
        val originalPriority = android.os.Process.getThreadPriority(processId)
        if (originalPriority > worstTargetProcessPriority) {
            android.os.Process.setThreadPriority(worstTargetProcessPriority)
            android.util.Log.v("WebPageLoader", "maintaining thread priority: " +
                    "$originalPriority -> ${android.os.Process.getThreadPriority(processId)}")
        } else {
            android.util.Log.v("WebPageLoader", "maintaining thread priority: " +
                    "$originalPriority")
        }
    }

    private var loadingTimeoutTimer: TimerTask = Timer().schedule(0) {}

    private fun startLoadingTimeoutTimer(webView: WebView) {
        val loadingTimeoutMillis = 15000L
        loadingTimeoutTimer = Timer().schedule(loadingTimeoutMillis) {
                    GlobalScope.launch(Dispatchers.Main) {
                        Log(webView.context, "Web page loading timeout\n" +
                                "exceeded $loadingTimeoutMillis ms: ${webView.url}").logError()
                        // todo use; self interrupt download
                    }
                }
    }

    private fun cancelLoadingTimeoutTimer(webView: WebView) {
        loadingTimeoutTimer.cancel()
        webView.stopLoading()
    }
}
