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
import com.alexvt.integrity.activity.FolderLocationsActivity
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.SnapshotStatus
import com.alexvt.integrity.core.job.JobProgress
import com.jakewharton.rxbinding3.widget.checkedChanges
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
        rvOfflineLinkList.adapter = OfflineLinkRecyclerAdapter(arrayListOf(), this)

        if (snapshotDataExists(intent)) {
            snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(
                    getArtifactIdFromIntent(intent), getDateFromIntent(intent))

            // Incomplete snapshot can be completed, apart from creating a new blueprint from it
            if (snapshot.status == SnapshotStatus.INCOMPLETE) {
                toolbar.title = "Incomplete Blog Type Snapshot"
                bContinueSaving.visibility = View.VISIBLE
            } else {
                if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
                    toolbar.title = "Viewing Blog Type Snapshot (downloading)"
                    showSnapshotSavingProgress()
                } else { // COMPLETE
                    toolbar.title = "Viewing Blog Type Snapshot"
                }
                bContinueSaving.visibility = View.GONE
            }

            fillInOptions(isEditable = false)

            GlobalScope.launch (Dispatchers.Main) {
                val snapshotPath = IntegrityCore.fetchSnapshotData(snapshot.artifactId,
                        snapshot.date)
                val linkToArchivePathRedirectMap = WebArchiveFilesUtil
                        .getPageIndexLinkToArchivePathMap(snapshotPath, "file://$snapshotPath/")
                (rvOfflineLinkList.adapter as OfflineLinkRecyclerAdapter)
                        .setItems(linkToArchivePathRedirectMap
                                .map { OfflineLink(it.key, it.value) })
                val firstArchivePath = linkToArchivePathRedirectMap.entries.firstOrNull()?.value
                        ?: "file:blank" // todo replace
                WebViewUtil.loadHtml(webView,
                        firstArchivePath,
                        linkToArchivePathRedirectMap,
                        getTypeMetadata().loadImages,
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

        bManageArchiveLocations.isEnabled = isEditable
        bManageArchiveLocations.setOnClickListener { openArchiveLocationList() }

        supportActionBar!!.subtitle = snapshot.title

        etRelatedLinkFilter.isEnabled = isEditable
        etRelatedLinkFilter.append(getTypeMetadata().relatedPageLinksFilter)
        etRelatedLinkFilter.textChanges()
                .debounce(800, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateMatchedRelatedLinkList() }
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

        sLinkedPagination.isEnabled = isEditable
        sLinkedPagination.isChecked = isLinkedPagination()
        sLinkedPagination.checkedChanges()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // indexed pagination when editable and not checked
                    etIndexedPaginationPattern.isEnabled = isEditable && !it
                    etIndexedPaginationStartIndex.isEnabled = isEditable && !it
                    etIndexedPaginationStep.isEnabled = isEditable && !it
                    etIndexedPaginationLimit.isEnabled = isEditable && !it
                    // linked pagination when editable and checked
                    etLinkedPaginationLinkFilter.isEnabled = isEditable && it
                    etLinkedPaginationLimit.isEnabled = isEditable && it
                }

        val snapshotPagination = getTypeMetadata().pagination
        val initialIndexedPagination = if (snapshotPagination is IndexedPagination)
            snapshotPagination else IndexedPagination()
        val initialLinkedPagination = if (snapshotPagination is LinkedPagination)
            snapshotPagination else LinkedPagination()
        etIndexedPaginationLimit.setText(initialIndexedPagination.limit.toString())
        etIndexedPaginationStep.setText(initialIndexedPagination.step.toString())
        etIndexedPaginationPattern.setText(initialIndexedPagination.path)
        etIndexedPaginationStartIndex.setText(initialIndexedPagination.startIndex.toString())
        etLinkedPaginationLinkFilter.setText(initialLinkedPagination.nextPageLinkFilter)
        etLinkedPaginationLimit.setText(initialLinkedPagination.limit.toString())

        etLoadInterval.isEnabled = isEditable
        etLoadInterval.setText((getTypeMetadata().loadIntervalMillis / 1000).toString())

        tvPageLinksPreview.visibility = if (isEditable) View.VISIBLE else View.GONE
        rvRelatedLinkList.visibility = if (isEditable) View.VISIBLE else View.GONE

        tvOfflineLinks.visibility = if (isEditable) View.GONE else View.VISIBLE
        rvOfflineLinkList.visibility = if (isEditable) View.GONE else View.VISIBLE

        bSaveBlueprint.setOnClickListener {
            if (checkAndSaveBlueprint()) {
                finish()
            }
        }
        bSave.visibility = if (isEditable) View.VISIBLE else View.GONE
        bSave.setOnClickListener {
            if (checkAndSaveBlueprint()) {
                saveSnapshotAndShowProgress()
            }
        }
        bContinueSaving.setOnClickListener { saveSnapshotAndShowProgress() }
    }

    fun getPaginationFromOptions() = if (sLinkedPagination.isChecked) {
        LinkedPagination(
                nextPageLinkFilter = etLinkedPaginationLinkFilter.text.toString(),
                limit = etLinkedPaginationLimit.text.toString().toInt()
        )
    } else {
        IndexedPagination(
                path = etIndexedPaginationPattern.text.toString(),
                startIndex = etIndexedPaginationStartIndex.text.toString().toInt(),
                step = etIndexedPaginationStep.text.toString().toInt(),
                limit = etIndexedPaginationLimit.text.toString().toInt()
        )
    }

    fun isLinkedPagination() = getTypeMetadata().pagination is LinkedPagination

    fun openArchiveLocationList() {
        startActivity(Intent(this, FolderLocationsActivity::class.java))
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

    fun getArchiveLocationsText(folderLocations: Collection<FolderLocation>)
            = IntegrityCore.getNamedFolderLocationMap(folderLocations).keys
            .joinToString(", ")

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
            val allLinkMap = LinkUtil.ccsSelectLinks(loadedHtml, "", "", webView.url)
            val matchedLinkMap = LinkUtil.ccsSelectLinks(loadedHtml,
                    etLinkPattern.text.toString(), etRelatedLinkFilter.text.toString(), webView.url)
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
                            webView.url.trimEnd('/') // for editable snapshot, the one from loaded WebView
                        },
                        loadImages = cbLoadImages.isChecked,
                        desktopSite = cbDesktopSite.isChecked,
                        paginationUsed = cbUsePagination.isChecked,
                        pagination = getPaginationFromOptions(),
                        relatedPageLinksUsed = cbUseRelatedLinks.isChecked,
                        relatedPageLinksFilter = etRelatedLinkFilter.text.toString(),
                        relatedPageLinksPattern = etLinkPattern.text.toString(),
                        loadIntervalMillis = etLoadInterval.text.toString().toInt() * 1000L
                )
        )
        IntegrityCore.saveSnapshotBlueprint(snapshot)
        return true
    }

    fun saveSnapshotAndShowProgress() {
        IntegrityCore.createSnapshotFromBlueprint(snapshot.artifactId, snapshot.date)
        showSnapshotSavingProgress()
    }

    fun showSnapshotSavingProgress() {
        val materialDialogProgress = MaterialDialog(this)
                .title(text = "Creating snapshot of " + snapshot.title)
                .cancelable(false)
                .positiveButton(text = "In background") {
                    finish() // todo track job elsewhere, listen to data changes
                }
        materialDialogProgress.show()
        IntegrityCore.subscribeToJobProgress(snapshot.artifactId, snapshot.date) {
            onJobProgress(it, materialDialogProgress)
        }
        materialDialogProgress.negativeButton(text = "Stop") {
            IntegrityCore.cancelSnapshotCreation(snapshot.artifactId, snapshot.date)
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

    fun goToOfflinePageDirectly(urlToView: String) {
        webView.loadUrl(urlToView) // todo use WebViewUtil; show current URL above when offline or editing
        dlAllContent.closeDrawers()
    }

    fun goToWebPage(urlToView: String): Boolean {
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl(urlToView), emptyMap(),
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
