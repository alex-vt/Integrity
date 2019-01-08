/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.alexvt.integrity.lib.DataTypeActivity
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.lib.util.LinkUtil
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebViewUtil
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.SnapshotMetadata
import com.alexvt.integrity.lib.SnapshotStatus
import kotlinx.android.synthetic.main.activity_github_type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class GitHubTypeActivity : DataTypeActivity() {

    private val TAG = GitHubTypeActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_github_type)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        if (snapshotViewMode()) {
            snapshot = IntegrityEx.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)

            // Incomplete snapshot can be completed, apart from creating a new blueprint from it
            if (snapshot.status == SnapshotStatus.INCOMPLETE) {
                toolbar.title = "Incomplete GitHub Type Snapshot"
                bContinueSaving.visibility = View.VISIBLE
            } else {
                toolbar.title = "Viewing GitHub Type Snapshot"
                bContinueSaving.visibility = View.GONE
            }

            fillInOptions(isEditable = false)

            GlobalScope.launch (Dispatchers.Main) {
                val snapshotPath = IntegrityEx.getSnapshotDataFolderPath(applicationContext,
                        snapshot.artifactId, snapshot.date)
                val linkToArchivePathRedirectMap = WebArchiveFilesUtil
                        .getPageIndexLinkToArchivePathMap(applicationContext, snapshotPath,
                                "file://$snapshotPath/")
                val firstArchivePath = linkToArchivePathRedirectMap.entries.firstOrNull()?.value
                        ?: "file:blank" // todo replace
                WebViewUtil.loadHtml(webView,
                        firstArchivePath,
                        linkToArchivePathRedirectMap,
                        true,
                        false) {
                    Log.d(TAG, "Loaded HTML from file")
                }
            }

        } else if (snapshotCreateMode()) {
            snapshot = IntegrityEx.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)
            toolbar.title = "Creating new GitHub Type Snapshot"

            fillInOptions(isEditable = true)

            goToGitHubUserPage(etUserName.text.toString())

        } else {
            snapshot = SnapshotMetadata(
                    artifactId = System.currentTimeMillis(),
                    title = "GitHub Type Artifact",
                    dataTypeSpecificMetadata = GitHubTypeMetadata()
            )

            toolbar.title = "Creating new GitHub Type Artifact"

            fillInOptions(isEditable = true)
        }
    }

    override fun updateFolderLocationSelectionInViews(folderLocationTexts: Array<String>) {
        tvArchiveLocations.text = folderLocationTexts.joinToString(separator = ", ")
    }

    override fun updateDownloadScheduleInViews(optionText: String) {
        tvDownloadSchedule.text = optionText
    }

    override fun checkSnapshot(status: String): Boolean {
        if (etUserName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter uer name first", Toast.LENGTH_SHORT).show()
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
                dataTypeSpecificMetadata = GitHubTypeMetadata(
                        userName = if (snapshotViewMode()) {
                            getTypeMetadata().userName // for read only snapshot, same as it was
                        } else {
                            etUserName.text.toString().trim('/', ' ')
                        }
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

    fun fillInOptions(isEditable: Boolean) {
        etUserName.isEnabled = isEditable
        etUserName.setText(LinkUtil.getShortFormUrl(getUserName()))
        etUserName.setOnEditorActionListener { v, actionId, event -> goToGitHubUserPage(etUserName.text.toString()) }
        bGo.isEnabled = isEditable
        bGo.setOnClickListener { view -> goToGitHubUserPage(etUserName.text.toString()) }

        etName.isEnabled = isEditable
        etName.append(snapshot.title)
        etDescription.isEnabled = isEditable
        etDescription.append(snapshot.description)

        updateFolderLocationSelectionInViews(IntentUtil.getFolderLocationNames(intent))
        bArchiveLocation.isEnabled = isEditable
        bArchiveLocation.setOnClickListener { openFolderLocationList(selectMode = true) }

        bManageArchiveLocations.isEnabled = isEditable
        bManageArchiveLocations.setOnClickListener { openFolderLocationList(selectMode = false) }

        tvDownloadSchedule.text = getDownloadScheduleText(snapshot.downloadSchedule)
        bDownloadSchedule.isEnabled = isEditable
        bDownloadSchedule.setOnClickListener { askSetDownloadSchedule() }

        sDownloadOnWifi.isEnabled = isEditable
        sDownloadOnWifi.isChecked = snapshot.downloadSchedule.allowOnWifi
        sDownloadOnMobileData.isEnabled = isEditable
        sDownloadOnMobileData.isChecked = snapshot.downloadSchedule.allowOnMobileData

        supportActionBar!!.subtitle = snapshot.title

        bSaveBlueprint.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.BLUEPRINT) }
        bSave.visibility = if (isEditable) View.VISIBLE else View.GONE
        bSave.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.IN_PROGRESS) }
        bContinueSaving.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.INCOMPLETE) }
    }

    // map of related page links to their unique CSS selectors in HTML document
    private var loadedHtml = ""

    fun getTypeMetadata(): GitHubTypeMetadata
            = snapshot.dataTypeSpecificMetadata as GitHubTypeMetadata

    // URL of first (main) web page of the latest snapshot of this artifact
    fun getUserName(): String = getTypeMetadata().userName

    fun goToGitHubUserPage(userName: String): Boolean {
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl("https://github.com/" + userName),
                emptyMap(), true, false) {
            Log.d(TAG, "Loaded page from: ${webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (artifactCreateMode()) {
                etName.setText(userName)
                supportActionBar!!.subtitle = webView.title
            }
        }
        return false
    }

}
