/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.integrity.R
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import kotlinx.android.synthetic.main.activity_blog_type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet


class BlogTypeActivity : AppCompatActivity() {

    private val TAG = BlogTypeActivity::class.java.simpleName

    // destinations for saving snapshot
    private val newSelectedArchiveLocations: LinkedHashSet<FolderLocation> = linkedSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blog_type)
        setSupportActionBar(toolbar)

        setupWebView(webView)

        // when it's an existing artifact, using existing URL instead of address bar
        val existingArtifactId = getArtifactIdFromIntent(intent)
        val snapshotDate = getDateFromIntent(intent)

        if (existingArtifactId >= 0 && !snapshotDate.isEmpty()) {
            toolbar.title = "Viewing Blog Type Snapshot"
            etShortUrl.visibility = View.GONE
            bGo.visibility = View.GONE
            llBottomSheet.visibility = View.GONE
            val snapshotDataPath = IntegrityCore.fetchSnapshotData(existingArtifactId, snapshotDate)
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY // no loading from internet
            webView.loadUrl("file://" + snapshotDataPath + "/index.mht")

        } else if (existingArtifactId >= 0) {
            toolbar.title = "Creating new Blog Type Snapshot"
            // todo make snapshot data editable
            etShortUrl.isEnabled = false
            etName.isEnabled = false
            etDescription.isEnabled = false
            bArchiveLocation.isEnabled = false
            bGo.isEnabled = false
            etShortUrl.setText(getShortFormUrl(getLatestSnapshotUrl(existingArtifactId)))
            etName.append(getLatestSnapshot(existingArtifactId).title)
            etDescription.append(getLatestSnapshot(existingArtifactId).description)
            tvArchiveLocations.text = getArchiveLocationsText(getLatestSnapshot(existingArtifactId)
                    .archiveFolderLocations)
            bSave.setOnClickListener { savePageAsSnapshot(existingArtifactId, webView) }
            goToWebPage(etShortUrl.text.toString())

        } else {
            toolbar.title = "Creating new Blog Type Artifact"
            etShortUrl.setOnEditorActionListener { v, actionId, event -> goToWebPage(etShortUrl.text.toString()) }
            bArchiveLocation.setOnClickListener { askAddArchiveLocation() }
            bGo.setOnClickListener { view -> goToWebPage(etShortUrl.text.toString()) }
            bSave.setOnClickListener { savePageAsNewArtifact(webView) }
        }
    }

    fun askAddArchiveLocation() {
        MaterialDialog(this)
                .listItems(items = ArrayList(IntegrityCore.getNamedFolderLocationMap().keys)) {
                    _, _, text -> addArchiveLocationSelection(IntegrityCore
                        .getNamedFolderLocationMap()[text]!!)
                }
                .show()
    }

    fun addArchiveLocationSelection(folderLocation: FolderLocation) {
        newSelectedArchiveLocations.add(folderLocation)
        tvArchiveLocations.text = getArchiveLocationsText(newSelectedArchiveLocations)
    }

    fun getArchiveLocationsText(folderLocations: Collection<FolderLocation>): String
            = IntegrityCore.getNamedFolderLocationMap(folderLocations).keys.toString()
            .replace("[", "")
            .replace("]", "")

    fun getArtifactIdFromIntent(intent: Intent): Long {
        return intent.getLongExtra("artifactId", -1)
    }

    fun getDateFromIntent(intent: Intent): String {
        var date: String? = intent.getStringExtra("date")
        if (date == null) {
            date = ""
        }
        return date
    }

    fun setupWebView(webView: WebView) {
        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.saveFormData = false
        webView.settings.loadsImagesAutomatically = true
        webView.settings.setAppCacheEnabled(false)
        webView.clearHistory()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, urlNewString: String): Boolean {
                webView.loadUrl(urlNewString)
                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {}

            override fun onPageFinished(view: WebView, url: String) {
                Log.d(TAG, "Page loaded: $url")
                etShortUrl.setText(getShortFormUrl(url))
            }
        }
    }

    fun getLatestSnapshot(artifactId: Long): SnapshotMetadata = IntegrityCore
            .metadataRepository.getLatestSnapshotMetadata(artifactId)

    // URL of first (main) web page of the latest snapshot of this artifact
    fun getLatestSnapshotUrl(artifactId: Long): String
            = (getLatestSnapshot(artifactId).dataTypeSpecificMetadata as BlogTypeMetadata).urls[0]

    fun savePageAsNewArtifact(view: WebView) {
        if (webView.url == null || !webView.url.startsWith("http") || webView.url.length < 10) {
            Toast.makeText(this, "Please go to a web page first", Toast.LENGTH_SHORT).show()
            return
        }
        if (etName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
            return
        }
        if (newSelectedArchiveLocations.isEmpty()) {
            Toast.makeText(this, "Please add location where to save archive", Toast.LENGTH_SHORT).show()
            return
        }
        GlobalScope.launch (Dispatchers.Main) {
            IntegrityCore.createArtifact(title = etName.text.toString(),
                    description = etDescription.text.toString(),
                    dataArchiveLocations = ArrayList(newSelectedArchiveLocations),
                    dataTypeSpecificMetadata = BlogTypeMetadata(arrayListOf(webView.url)),
                    webViewWithContent = view)
            finish()
        }
    }

    fun savePageAsSnapshot(existingArtifactId: Long, view: WebView) {
        GlobalScope.launch (Dispatchers.Main) {
            IntegrityCore.createSnapshot(existingArtifactId, view)
            finish()
        }
    }


    fun getShortFormUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url.replaceFirst("http://", "").replaceFirst("https://", "")
        }
        return url
    }

    fun getFullFormUrl(url: String): String {
        return if (!url.startsWith("https://") && !url.startsWith("http://")) {
            "http://" + url
        } else {
            url
        }
    }

    fun goToWebPage(urlToView: String): Boolean {
        webView.loadUrl(getFullFormUrl(urlToView))
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_delete_all -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
