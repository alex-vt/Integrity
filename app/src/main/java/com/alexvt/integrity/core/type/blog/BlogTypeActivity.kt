/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.integrity.R
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_blog_type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class BlogTypeActivity : AppCompatActivity() {

    private val TAG = BlogTypeActivity::class.java.simpleName

    // preliminary (intended) snapshot metadata to view/save related data
    private lateinit var snapshotBlueprint: SnapshotMetadata

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blog_type)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        // when it's an existing artifact, using existing URL instead of address bar
        val existingArtifactId = getArtifactIdFromIntent(intent)
        val snapshotDate = getDateFromIntent(intent)

        rvRelatedLinkList.adapter = RelatedLinkRecyclerAdapter(arrayListOf(), this)

        if (existingArtifactId >= 0 && !snapshotDate.isEmpty()) {
            toolbar.title = "Viewing Blog Type Snapshot"
            snapshotBlueprint = IntegrityCore.metadataRepository.getSnapshotMetadata(existingArtifactId, snapshotDate)

            fillInOptions(isEditable = false)
            llBottomSheet.visibility = View.GONE
            tvPageLinksPreview.visibility = View.GONE

            GlobalScope.launch (Dispatchers.Main) {
                // links from locally loaded HTML can point to local pages named page_<linkHash>
                val relatedLinkHashesFromFiles = DataCacheFolderUtil.getSnapshotFileSimpleNames(
                        existingArtifactId, snapshotDate)
                        .map { it.replace("page_", "") }
                val snapshotDataPath = IntegrityCore.fetchSnapshotData(existingArtifactId, snapshotDate)
                WebViewUtil.loadHtml(webView,"file://" + snapshotDataPath + "/index.mht",
                        relatedLinkHashesFromFiles) {
                    Log.d(TAG, "Loaded HTML from file")
                }
            }


        } else if (existingArtifactId >= 0) {
            toolbar.title = "Creating new Blog Type Snapshot"
            snapshotBlueprint = IntegrityCore.metadataRepository.getLatestSnapshotMetadata(existingArtifactId)
            
            // disabling pagination by default for manual snapshot creation
            if (getBlueprintTypeMetadata().paginationUsed) {
                snapshotBlueprint = snapshotBlueprint.copy(
                        dataTypeSpecificMetadata = getBlueprintTypeMetadata().copy(
                                paginationUsed = false
                        )
                )
                Toast.makeText(this, "Pagination is turned off", Toast.LENGTH_SHORT).show()
            }

            fillInOptions(isEditable = true)

            goToWebPage(etShortUrl.text.toString())

        } else {
            toolbar.title = "Creating new Blog Type Artifact"
            snapshotBlueprint = SnapshotMetadata(
                    artifactId = System.currentTimeMillis(),
                    title = "Blog Type Artifact"
            )

            fillInOptions(isEditable = true)
        }
    }

    fun fillInOptions(isEditable: Boolean) {
        etShortUrl.setText(LinkUtil.getShortFormUrl(getLatestSnapshotUrl()))
        etShortUrl.setOnEditorActionListener { v, actionId, event -> goToWebPage(etShortUrl.text.toString()) }
        bGo.setOnClickListener { view -> goToWebPage(etShortUrl.text.toString()) }

        etName.append(snapshotBlueprint.title)
        etDescription.append(snapshotBlueprint.description)

        tvArchiveLocations.text = getArchiveLocationsText(snapshotBlueprint.archiveFolderLocations)
        bArchiveLocation.setOnClickListener { askAddArchiveLocation() }

        supportActionBar!!.subtitle = snapshotBlueprint.title

        etLinkPattern.isEnabled = isEditable
        etLinkPattern.append(getBlueprintTypeMetadata().relatedPageLinksPattern)
        etLinkPattern.textChanges()
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateMatchedRelatedLinkList() }

        cbUseRelatedLinks.isEnabled = isEditable
        cbUseRelatedLinks.isChecked = getBlueprintTypeMetadata().relatedPageLinksUsed
        cbUsePagination.isEnabled = isEditable
        cbUsePagination.isChecked = getBlueprintTypeMetadata().paginationUsed
        etPaginationPattern.isEnabled = isEditable
        etPaginationPattern.setText(getBlueprintTypeMetadata().pagination.path)
        etPaginationStartIndex.isEnabled = isEditable
        etPaginationStartIndex.setText(getBlueprintTypeMetadata().pagination.startIndex.toString())
        etPaginationStep.isEnabled = isEditable
        etPaginationStep.setText(getBlueprintTypeMetadata().pagination.step.toString())
        etPaginationLimit.isEnabled = isEditable
        etPaginationLimit.setText(getBlueprintTypeMetadata().pagination.limit.toString())
        etLoadInterval.isEnabled = isEditable
        etLoadInterval.setText((getBlueprintTypeMetadata().loadIntervalMillis / 1000).toString())

        bSaveBlueprint.setOnClickListener {
            if (checkAndSaveBlueprint()) {
                finish()
            }
        }
        bSave.setOnClickListener { saveSnapshotAndFinish() }
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
        snapshotBlueprint = snapshotBlueprint.copy(
                archiveFolderLocations = ArrayList(snapshotBlueprint.archiveFolderLocations.plus(folderLocation))
        )
        tvArchiveLocations.text = getArchiveLocationsText(snapshotBlueprint.archiveFolderLocations)
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

    fun getBlueprintTypeMetadata(): BlogTypeMetadata
            = snapshotBlueprint.dataTypeSpecificMetadata as BlogTypeMetadata

    // URL of first (main) web page of the latest snapshot of this artifact
    fun getLatestSnapshotUrl(): String = getBlueprintTypeMetadata().url

    fun checkAndSaveBlueprint(): Boolean {
        if (webView.url == null || !webView.url.startsWith("http") || webView.url.length < 10) {
            Toast.makeText(this, "Please go to a web page first", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (snapshotBlueprint.archiveFolderLocations.isEmpty()) {
            Toast.makeText(this, "Please add location where to save archive", Toast.LENGTH_SHORT).show()
            return false
        }
        val timestamp = System.currentTimeMillis()
        // for new artifact, generating artifactId
        if (snapshotBlueprint.artifactId < 1) {
            snapshotBlueprint = snapshotBlueprint.copy(artifactId = timestamp)
        }
        snapshotBlueprint = snapshotBlueprint.copy(
                title = etName.text.toString(),
                date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(timestamp),
                description = etDescription.text.toString(),
                dataTypeSpecificMetadata = BlogTypeMetadata(
                        url = webView.url,
                        paginationUsed = cbUsePagination.isChecked,
                        pagination = Pagination(
                                path = etPaginationPattern.text.toString(),
                                startIndex = etPaginationStartIndex.text.toString().toInt(),
                                step = etPaginationStep.text.toString().toInt(),
                                limit = etPaginationLimit.text.toString().toInt()
                        ),
                        relatedPageLinksUsed = cbUseRelatedLinks.isChecked,
                        relatedPageLinksPattern = etLinkPattern.text.toString(),
                        loadIntervalMillis = etLoadInterval.text.toString().toInt() * 1000L
                )
        )
        IntegrityCore.saveSnapshotBlueprint(snapshotBlueprint)
        return true
    }

    fun saveSnapshotAndFinish() {
        if (!checkAndSaveBlueprint()) {
            return
        }
        val materialDialogProgress = MaterialDialog(this)
                .title(text = "Creating snapshot of " + snapshotBlueprint.title)
                .cancelable(false)
                .positiveButton(text = "In background") {
                    finish() // todo track job elsewhere, listen to data changes
                }
        materialDialogProgress.show()
        val job = IntegrityCore.createSnapshotFromBlueprint(snapshotBlueprint.artifactId) {
            onJobProgress(it, materialDialogProgress)
        }
        materialDialogProgress.negativeButton(text = "Cancel") {
            IntegrityCore.cancelJob(job)
        }
    }

    fun onJobProgress(jobProgress: JobProgress<SnapshotMetadata>, dialog: MaterialDialog) {
        dialog.message(text = jobProgress.progressMessage)
        if (jobProgress.result != null) {
            dialog.cancel()
            finish()
        }
    }

    fun goToWebPage(urlToView: String): Boolean {
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl(urlToView), setOf()) {
            Log.d(TAG, "Loaded page from: ${webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (getArtifactIdFromIntent(intent) < 1) {
                etShortUrl.text.clear()
                etShortUrl.append(LinkUtil.getShortFormUrl(webView.url))
                etName.setText(webView.title)
                supportActionBar!!.subtitle = webView.title
            }
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
        menuInflater.inflate(R.menu.menu_data_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_options -> {
                dlAllContent.openDrawer(Gravity.RIGHT)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
