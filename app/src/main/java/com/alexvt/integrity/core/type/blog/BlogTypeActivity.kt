/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.integrity.R
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.TypeMetadata
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_blog_type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
        supportActionBar!!.setDisplayHomeAsUpEnabled(true);
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        // when it's an existing artifact, using existing URL instead of address bar
        val existingArtifactId = getArtifactIdFromIntent(intent)
        val snapshotDate = getDateFromIntent(intent)

        rvRelatedLinkList.adapter = RelatedLinkRecyclerAdapter(arrayListOf(), this)

        if (existingArtifactId >= 0 && !snapshotDate.isEmpty()) {
            toolbar.title = "Viewing Blog Type Snapshot"
            llBottomSheet.visibility = View.GONE
            etLinkPattern.isEnabled = false
            etLinkPattern.append((getLatestSnapshot(existingArtifactId).dataTypeSpecificMetadata as BlogTypeMetadata)
                    .relatedPageLinksPattern)
            tvPageLinksPreview.visibility = View.GONE
            val snapshotDataPath = IntegrityCore.fetchSnapshotData(existingArtifactId, snapshotDate)
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ONLY // no loading from internet
            GlobalScope.launch (Dispatchers.Main) {
                // links from locally loaded HTML can point to local pages named page_<linkHash>
                val relatedLinkHashesFromFiles = DataCacheFolderUtil.getSnapshotFileSimpleNames(
                        existingArtifactId, snapshotDate)
                        .map { it.replace("page_", "") }
                WebViewUtil.loadHtml(webView,"file://" + snapshotDataPath + "/index.mht",
                        relatedLinkHashesFromFiles) {
                    Log.d(TAG, "Loaded HTML from file")
                }
            }

        } else if (existingArtifactId >= 0) {
            toolbar.title = "Creating new Blog Type Snapshot"
            // todo make snapshot data editable
            etShortUrl.isEnabled = false
            etName.isEnabled = false
            etDescription.isEnabled = false
            bArchiveLocation.isEnabled = false
            bGo.isEnabled = false
            etLinkPattern.isEnabled = false
            etShortUrl.setText(LinkUtil.getShortFormUrl(getLatestSnapshotUrl(existingArtifactId)))
            etName.append(getLatestSnapshot(existingArtifactId).title)
            etDescription.append(getLatestSnapshot(existingArtifactId).description)
            tvArchiveLocations.text = getArchiveLocationsText(getLatestSnapshot(existingArtifactId)
                    .archiveFolderLocations)
            etLinkPattern.append((getLatestSnapshot(existingArtifactId).dataTypeSpecificMetadata as BlogTypeMetadata)
                    .relatedPageLinksPattern)
            bSave.setOnClickListener { savePageAsSnapshot(existingArtifactId) }
            goToWebPage(etShortUrl.text.toString())

        } else {
            toolbar.title = "Creating new Blog Type Artifact"
            etShortUrl.setOnEditorActionListener { v, actionId, event -> goToWebPage(etShortUrl.text.toString()) }
            bArchiveLocation.setOnClickListener { askAddArchiveLocation() }
            bGo.setOnClickListener { view -> goToWebPage(etShortUrl.text.toString()) }
            bSave.setOnClickListener { savePageAsNewArtifact(webView) }
            etLinkPattern.textChanges()
                    .debounce(800, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { updateMatchedRelatedLinkList() }
        }
    }

    fun askAddArchiveLocation() {
        MaterialDialog(this)
                .listItems(items = ArrayList(IntegrityCore.getNamedFolderLocationMap().keys)) { _, _, text ->
                    addArchiveLocationSelection(IntegrityCore
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

    fun getRelatedLinksPatternText(typeMetadata: TypeMetadata): String
            = getRelatedLinksPatternText((typeMetadata as BlogTypeMetadata).relatedPageLinksPattern)

    fun getRelatedLinksPatternText(cssSelector: String): String
            = "Pattern of related links to save:\n" +
            if (cssSelector.isNotEmpty()) cssSelector else "(none)"

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

    fun updateMatchedRelatedLinkList() {
        try {
            val allLinkMap = LinkUtil.getCssSelectedLinkMap(loadedHtml, "", webView.url)
            val matchedLinkMap = LinkUtil.getCssSelectedLinkMap(loadedHtml,
                    etLinkPattern.text.toString(), webView.url)
            val unmatchedLinkMap = allLinkMap.minus(matchedLinkMap)
            // marched shown first
            (rvRelatedLinkList.adapter as RelatedLinkRecyclerAdapter).setItems(
                    matchedLinkMap.map { it -> MatchableLink(it.key, it.value, true) }
                            .plus(unmatchedLinkMap.map { it -> MatchableLink(it.key as String, it.value, false) })
            )
            Log.d(TAG, "Matched links: ${matchedLinkMap.size} of ${allLinkMap.size}")
        } catch (t: Throwable) {
            Log.e(TAG, "updateMatchedRelatedLinkList exception", t)
        }
    }

    // map of related page links to their unique CSS selectors in HTML document
    private var loadedHtml = ""

    fun getLatestSnapshot(artifactId: Long): SnapshotMetadata = IntegrityCore
            .metadataRepository.getLatestSnapshotMetadata(artifactId)

    // URL of first (main) web page of the latest snapshot of this artifact
    fun getLatestSnapshotUrl(artifactId: Long): String
            = (getLatestSnapshot(artifactId).dataTypeSpecificMetadata as BlogTypeMetadata).url

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
        val materialDialogProgress = MaterialDialog(this)
                .title(text = "Saving first snapshot of a new artifact " + etName.text.toString())
                .cancelable(false)
        materialDialogProgress.show()
        IntegrityCore.createArtifact(title = etName.text.toString(),
                description = etDescription.text.toString(),
                dataArchiveLocations = ArrayList(newSelectedArchiveLocations),
                dataTypeSpecificMetadata = BlogTypeMetadata(webView.url,
                        etLinkPattern.text.toString())) {
            materialDialogProgress.message(text = it.progressMessage)
            if (it.result != null) {
                materialDialogProgress.cancel()
                finish()
            }
        }
    }

    fun savePageAsSnapshot(existingArtifactId: Long) {
        val materialDialogProgress = MaterialDialog(this)
                .title(text = "Saving a new snapshot of this artifact")
                .cancelable(false)
        materialDialogProgress.show()
        IntegrityCore.createSnapshot(existingArtifactId) {
            materialDialogProgress.message(text = it.progressMessage)
            if (it.result != null) {
                materialDialogProgress.cancel()
                finish()
            }
        }
    }

    fun goToWebPage(urlToView: String): Boolean {
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl(urlToView), setOf()) {
            Log.d(TAG, "Loaded page from: ${webView.url}")
            loadedHtml = it
            etShortUrl.setText(LinkUtil.getShortFormUrl(webView.url))
            updateMatchedRelatedLinkList()
        }
        return false
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_all -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
