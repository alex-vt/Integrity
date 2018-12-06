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
import com.alexvt.integrity.core.SnapshotStatus
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

    // snapshot metadata to view/save related data
    private lateinit var snapshot: SnapshotMetadata

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blog_type)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        rvRelatedLinkList.adapter = RelatedLinkRecyclerAdapter(arrayListOf(), this)

        if (snapshotDataExists(intent)) {
            snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(
                    getArtifactIdFromIntent(intent), getDateFromIntent(intent))

            // Incomplete snapshot can be completed, apart from creating a new blueprint from it
            if (snapshot.status == SnapshotStatus.INCOMPLETE) {
                toolbar.title = "Incomplete Blog Type Snapshot"
                bContinueSaving.visibility = View.VISIBLE
            } else {
                toolbar.title = "Viewing Blog Type Snapshot"
                bContinueSaving.visibility = View.GONE
            }

            fillInOptions(isEditable = false)

            GlobalScope.launch (Dispatchers.Main) {
                // links from locally loaded HTML can point to local pages named page_<linkHash>
                val relatedLinkHashesFromFiles = DataCacheFolderUtil.getSnapshotFileSimpleNames(
                        snapshot.artifactId, snapshot.date)
                        .map { it.replace("page_", "") }
                val snapshotDataPath = IntegrityCore.fetchSnapshotData(snapshot.artifactId,
                        snapshot.date)
                WebViewUtil.loadHtml(webView,"file://" + snapshotDataPath + "/index.mht",
                        relatedLinkHashesFromFiles, getTypeMetadata().loadImages,
                        getTypeMetadata().desktopSite) {
                    Log.d(TAG, "Loaded HTML from file")
                }
            }

        } else if (artifactExists(intent)) {
            snapshot = IntegrityCore.metadataRepository.getLatestSnapshotMetadata(
                    getArtifactIdFromIntent(intent))
            toolbar.title = "Creating new Blog Type Snapshot"

            // disabling pagination by default for manual snapshot creation
            if (getTypeMetadata().paginationUsed) {
                snapshot = snapshot.copy(
                        dataTypeSpecificMetadata = getTypeMetadata().copy(
                                paginationUsed = false
                        )
                )
                Toast.makeText(this, "Pagination is turned off", Toast.LENGTH_SHORT).show()
            }

            fillInOptions(isEditable = true)

            goToWebPage(etShortUrl.text.toString())

        } else {
            snapshot = SnapshotMetadata(
                    artifactId = System.currentTimeMillis(),
                    title = "Blog Type Artifact"
            )

            toolbar.title = "Creating new Blog Type Artifact"

            fillInOptions(isEditable = true)
        }
    }

    fun artifactExists(intent: Intent?) = getArtifactIdFromIntent(intent) > 0

    fun snapshotDataExists(intent: Intent?) = artifactExists(intent) && !getDateFromIntent(intent).isEmpty()
            && IntegrityCore.metadataRepository.getSnapshotMetadata(
            getArtifactIdFromIntent(intent), getDateFromIntent(intent))
            .status != SnapshotStatus.BLUEPRINT

    fun fillInOptions(isEditable: Boolean) {
        etShortUrl.isEnabled = isEditable
        etShortUrl.setText(LinkUtil.getShortFormUrl(getLatestSnapshotUrl()))
        etShortUrl.setOnEditorActionListener { v, actionId, event -> goToWebPage(etShortUrl.text.toString()) }
        bGo.isEnabled = isEditable
        bGo.setOnClickListener { view -> goToWebPage(etShortUrl.text.toString()) }

        etName.isEnabled = isEditable
        etName.append(snapshot.title)
        etDescription.isEnabled = isEditable
        etDescription.append(snapshot.description)

        tvArchiveLocations.text = getArchiveLocationsText(snapshot.archiveFolderLocations)
        bArchiveLocation.isEnabled = isEditable
        bArchiveLocation.setOnClickListener { askAddArchiveLocation() }

        supportActionBar!!.subtitle = snapshot.title

        etLinkPattern.isEnabled = isEditable
        etLinkPattern.append(getTypeMetadata().relatedPageLinksPattern)
        etLinkPattern.textChanges()
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateMatchedRelatedLinkList() }

        cbLoadImages.isEnabled = isEditable
        cbLoadImages.isChecked = getTypeMetadata().loadImages
        cbDesktopSite.isEnabled = isEditable
        cbDesktopSite.isChecked = getTypeMetadata().desktopSite
        // todo update web page with debounce. Fix these changes not applying on page reload

        cbUseRelatedLinks.isEnabled = isEditable
        cbUseRelatedLinks.isChecked = getTypeMetadata().relatedPageLinksUsed
        cbUsePagination.isEnabled = isEditable
        cbUsePagination.isChecked = getTypeMetadata().paginationUsed
        etPaginationPattern.isEnabled = isEditable
        etPaginationPattern.setText(getTypeMetadata().pagination.path)
        etPaginationStartIndex.isEnabled = isEditable
        etPaginationStartIndex.setText(getTypeMetadata().pagination.startIndex.toString())
        etPaginationStep.isEnabled = isEditable
        etPaginationStep.setText(getTypeMetadata().pagination.step.toString())
        etPaginationLimit.isEnabled = isEditable
        etPaginationLimit.setText(getTypeMetadata().pagination.limit.toString())
        etLoadInterval.isEnabled = isEditable
        etLoadInterval.setText((getTypeMetadata().loadIntervalMillis / 1000).toString())

        tvPageLinksPreview.visibility = if (isEditable) View.VISIBLE else View.GONE

        bSaveBlueprint.setOnClickListener {
            if (checkAndSaveBlueprint()) {
                finish()
            }
        }
        bSave.visibility = if (isEditable) View.VISIBLE else View.GONE
        bSave.setOnClickListener {
            if (checkAndSaveBlueprint()) {
                saveSnapshotAndFinish()
            }
        }
        bContinueSaving.setOnClickListener { saveSnapshotAndFinish() }
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
        snapshot = snapshot.copy(
                archiveFolderLocations = ArrayList(snapshot.archiveFolderLocations.plus(folderLocation))
        )
        tvArchiveLocations.text = getArchiveLocationsText(snapshot.archiveFolderLocations)
    }

    fun getArchiveLocationsText(folderLocations: Collection<FolderLocation>): String
            = IntegrityCore.getNamedFolderLocationMap(folderLocations).keys.toString()
            .replace("[", "")
            .replace("]", "")

    fun getArtifactIdFromIntent(intent: Intent?): Long {
        return intent?.getLongExtra("artifactId", -1) ?: -1
    }

    fun getDateFromIntent(intent: Intent?): String {
        var date: String? = intent?.getStringExtra("date")
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

    fun getTypeMetadata(): BlogTypeMetadata
            = snapshot.dataTypeSpecificMetadata as BlogTypeMetadata

    // URL of first (main) web page of the latest snapshot of this artifact
    fun getLatestSnapshotUrl(): String = getTypeMetadata().url

    fun checkAndSaveBlueprint(): Boolean {
        if (etShortUrl.text.trim().isEmpty()) {
            Toast.makeText(this, "Please go to a web page first", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (snapshot.archiveFolderLocations.isEmpty()) {
            Toast.makeText(this, "Please add location where to save archive", Toast.LENGTH_SHORT).show()
            return false
        }
        val timestamp = System.currentTimeMillis()
        // for new artifact, generating artifactId
        if (snapshot.artifactId < 1) {
            snapshot = snapshot.copy(artifactId = timestamp)
        }
        snapshot = snapshot.copy(
                title = etName.text.toString(),
                date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(timestamp),
                description = etDescription.text.toString(),
                dataTypeSpecificMetadata = BlogTypeMetadata(
                        url = if (snapshotDataExists(intent)) {
                            getTypeMetadata().url // for read only snapshot, same as it was
                        } else {
                            webView.url // for editable snapshot, the one from loaded WebView
                        },
                        loadImages = cbLoadImages.isChecked,
                        desktopSite = cbDesktopSite.isChecked,
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
        IntegrityCore.saveSnapshotBlueprint(snapshot)
        return true
    }

    fun saveSnapshotAndFinish() {
        val materialDialogProgress = MaterialDialog(this)
                .title(text = "Creating snapshot of " + snapshot.title)
                .cancelable(false)
                .positiveButton(text = "In background") {
                    finish() // todo track job elsewhere, listen to data changes
                }
        materialDialogProgress.show()
        val job = IntegrityCore.createSnapshotFromBlueprint(snapshot.artifactId, snapshot.date) {
            onJobProgress(it, materialDialogProgress)
        }
        materialDialogProgress.negativeButton(text = "Stop") {
            IntegrityCore.cancelJob(job)
            finish()
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
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl(urlToView), setOf(),
                getTypeMetadata().loadImages, getTypeMetadata().desktopSite) {
            Log.d(TAG, "Loaded page from: ${webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (!artifactExists(intent)) {
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
