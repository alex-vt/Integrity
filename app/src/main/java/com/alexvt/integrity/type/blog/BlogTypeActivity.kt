/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.DataTypeActivity
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.lib.util.LinkUtil
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebViewUtil
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.SnapshotMetadata
import com.alexvt.integrity.lib.SnapshotStatus
import com.jakewharton.rxbinding3.widget.checkedChanges
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_blog_type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class BlogTypeActivity : DataTypeActivity() {

    private val TAG = BlogTypeActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blog_type)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        rvRelatedLinkList.adapter = RelatedLinkRecyclerAdapter(arrayListOf(), this)
        rvOfflineLinkList.adapter = OfflineLinkRecyclerAdapter(arrayListOf(), this)

        if (snapshotViewMode()) {
            snapshot = IntegrityEx.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)

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
                val snapshotPath = IntegrityEx.getSnapshotDataFolderPath(applicationContext,
                        snapshot.artifactId, snapshot.date)
                val linkToArchivePathRedirectMap = WebArchiveFilesUtil
                        .getPageIndexLinkToArchivePathMap(applicationContext, snapshotPath,
                                "file://$snapshotPath/")
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

        } else if (snapshotCreateMode()) {
            snapshot = IntegrityEx.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)
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
                    title = "Blog Type Artifact",
                    dataTypeSpecificMetadata = BlogTypeMetadata()
            )

            toolbar.title = "Creating new Blog Type Artifact"

            fillInOptions(isEditable = true)
        }
    }

    override fun updateFolderLocationSelectionInViews(folderLocationText: String) {
        tvArchiveLocations.text = folderLocationText
    }

    override fun updateDownloadScheduleInViews(optionText: String) {
        tvDownloadSchedule.text = optionText
    }

    override fun checkSnapshot(status: String): Boolean {
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
        // todo either construct snapshot here completely or modify it on option change
        snapshot = snapshot.copy(
                title = etName.text.toString(),
                description = etDescription.text.toString(),
                downloadSchedule = snapshot.downloadSchedule.copy( // period is already set
                        allowOnWifi = sDownloadOnWifi.isChecked,
                        allowOnMobileData = sDownloadOnMobileData.isChecked
                ),
                // archive locations already set
                dataTypeSpecificMetadata = BlogTypeMetadata(
                        url = if (snapshotViewMode()) {
                            getTypeMetadata().url // for read only snapshot, same as it was
                        } else if (webView.url != null) {
                            webView.url.trim('/', ' ') // for editable snapshot, the one from loaded WebView
                        } else {
                            etShortUrl.text.toString().trim('/', ' ')
                        },
                        loadImages = cbLoadImages.isChecked,
                        desktopSite = cbDesktopSite.isChecked,
                        paginationUsed = cbUsePagination.isChecked,
                        pagination = getPaginationFromOptions(),
                        relatedPageLinksUsed = cbUseRelatedLinks.isChecked,
                        relatedPageLinksFilter = etRelatedLinkFilter.text.toString(),
                        relatedPageLinksPattern = etLinkPattern.text.toString(),
                        loadIntervalMillis = etLoadInterval.text.toString().toInt() * 1000L
                ),
                status = status
        )
        return true
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
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


    // type specific

    private fun fillInOptions(isEditable: Boolean) {
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

        tvDownloadSchedule.text = getDownloadScheduleText(snapshot.downloadSchedule)
        bDownloadSchedule.isEnabled = isEditable
        bDownloadSchedule.setOnClickListener { askSetDownloadSchedule() }

        sDownloadOnWifi.isEnabled = isEditable
        sDownloadOnWifi.isChecked = snapshot.downloadSchedule.allowOnWifi
        sDownloadOnMobileData.isEnabled = isEditable
        sDownloadOnMobileData.isChecked = snapshot.downloadSchedule.allowOnMobileData

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

        bSaveBlueprint.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.BLUEPRINT) }
        bSave.visibility = if (isEditable) View.VISIBLE else View.GONE
        bSave.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.IN_PROGRESS) }
        bContinueSaving.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.INCOMPLETE) }
    }

    private fun getPaginationFromOptions() = if (sLinkedPagination.isChecked) {
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

    fun goToOfflinePageDirectly(urlToView: String) {
        webView.loadUrl(urlToView) // todo use WebViewUtil; show current URL above when offline or editing
        dlAllContent.closeDrawers()
    }

    private fun isLinkedPagination() = getTypeMetadata().pagination is LinkedPagination

    private fun updateMatchedRelatedLinkList() {
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

    private fun getTypeMetadata(): BlogTypeMetadata
            = snapshot.dataTypeSpecificMetadata as BlogTypeMetadata

    // URL of first (main) web page of the latest snapshot of this artifact
    private fun getLatestSnapshotUrl(): String = getTypeMetadata().url

    private fun goToWebPage(urlToView: String): Boolean {
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl(urlToView), emptyMap(),
                getTypeMetadata().loadImages, getTypeMetadata().desktopSite) {
            Log.d(TAG, "Loaded page from: ${webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (artifactCreateMode()) {
                etShortUrl.text.clear()
                etShortUrl.append(LinkUtil.getShortFormUrl(webView.url))
                etName.setText(webView.title)
                supportActionBar!!.subtitle = webView.title
            }
            updateMatchedRelatedLinkList()
        }
        return false
    }
}
