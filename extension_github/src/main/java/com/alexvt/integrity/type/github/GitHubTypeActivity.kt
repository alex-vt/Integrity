/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

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
import com.alexvt.integrity.core.*
import com.alexvt.integrity.core.util.LinkUtil
import com.alexvt.integrity.core.util.WebViewUtil
import kotlinx.android.synthetic.main.activity_github_type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList


class GitHubTypeActivity : AppCompatActivity() {

    private val TAG = GitHubTypeActivity::class.java.simpleName

    // snapshot metadata to view/save related data
    private lateinit var snapshot: SnapshotMetadata

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_github_type)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        if (snapshotDataExists(intent)) {
            snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(
                    getArtifactIdFromIntent(intent), getDateFromIntent(intent))

            // Incomplete snapshot can be completed, apart from creating a new blueprint from it
            if (snapshot.status == SnapshotStatus.INCOMPLETE) {
                toolbar.title = "Incomplete GitHub Type Snapshot"
                bContinueSaving.visibility = View.VISIBLE
            } else {
                if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
                    toolbar.title = "Viewing GitHub Type Snapshot (downloading)"
                    showSnapshotSavingProgress(snapshot.artifactId, snapshot.date)
                } else { // COMPLETE
                    toolbar.title = "Viewing Blog Type Snapshot"
                }
                bContinueSaving.visibility = View.GONE
            }

            fillInOptions(isEditable = false)

            GlobalScope.launch (Dispatchers.Main) {
                // todo
            }

        } else if (artifactExists(intent)) {
            snapshot = IntegrityCore.metadataRepository.getLatestSnapshotMetadata(
                    getArtifactIdFromIntent(intent))
            toolbar.title = "Creating new GitHub Type Snapshot"

            fillInOptions(isEditable = true)

            goToGitHubUserPage(etUserName.text.toString())

        } else {
            snapshot = SnapshotMetadata(
                    artifactId = System.currentTimeMillis(),
                    title = "GitHub Type Artifact"
            )

            toolbar.title = "Creating new GitHub Type Artifact"

            fillInOptions(isEditable = true)
        }
    }

    fun artifactExists(intent: Intent?) = getArtifactIdFromIntent(intent) > 0

    fun snapshotDataExists(intent: Intent?) = artifactExists(intent) && !getDateFromIntent(intent).isEmpty()
            && IntegrityCore.metadataRepository.getSnapshotMetadata(
            getArtifactIdFromIntent(intent), getDateFromIntent(intent))
            .status != SnapshotStatus.BLUEPRINT

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

    fun openArchiveLocationList() {
        // todo startActivity(Intent(this, FolderLocationsActivity::class.java))
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

    // todo improve this option
    fun getDownloadScheduleText(downloadSchedule: DownloadSchedule)
            = "Every " + downloadSchedule.periodSeconds + " s"

    val downloadScheduleOptionMap = mapOf(
            Pair("Never", 0L),
            Pair("Every minute", 60L),
            Pair("Every hour", 60 * 60L),
            Pair("Every day", 24 * 60 * 60L),
            Pair("Every week", 7 * 24 * 60 * 60L),
            Pair("Every month", 30 * 7 * 24 * 60 * 60L)
    )

    fun askSetDownloadSchedule() {
        MaterialDialog(this)
                .listItems(items = ArrayList(downloadScheduleOptionMap.keys)) { _, _, text ->
                    setDownloadSchedule(text)
                }
                .show()
    }

    fun setDownloadSchedule(optionText: String) {
        snapshot = snapshot.copy(
                downloadSchedule = snapshot.downloadSchedule
                        .copy(periodSeconds = downloadScheduleOptionMap[optionText]!!)
        )
        tvDownloadSchedule.text = optionText
    }

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

    // map of related page links to their unique CSS selectors in HTML document
    private var loadedHtml = ""

    fun getTypeMetadata(): GitHubTypeMetadata
            = snapshot.dataTypeSpecificMetadata as GitHubTypeMetadata

    // URL of first (main) web page of the latest snapshot of this artifact
    fun getUserName(): String = getTypeMetadata().userName

    fun checkAndSaveBlueprint(): Boolean {
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
                        userName = if (snapshotDataExists(intent)) {
                            getTypeMetadata().userName // for read only snapshot, same as it was
                        } else {
                            etUserName.text.toString().trim('/', ' ')
                        }
                )
        )
        IntegrityCore.saveSnapshotBlueprint(snapshot)
        return true
    }

    fun saveSnapshotAndShowProgress() {
        val blueprintDate = IntegrityCore.metadataRepository
                .getLatestSnapshotMetadata(snapshot.artifactId).date
        IntegrityCore.createSnapshotFromBlueprint(snapshot.artifactId, blueprintDate)
        showSnapshotSavingProgress(snapshot.artifactId, blueprintDate)
    }

    fun showSnapshotSavingProgress(artifactId: Long, date: String) {
        IntegrityCore.showRunningJobProgressDialog(this, artifactId, date).setOnCancelListener {
            finish()
        }
    }

    fun goToOfflinePageDirectly(urlToView: String) {
        webView.loadUrl(urlToView) // todo use WebViewUtil; show current URL above when offline or editing
        dlAllContent.closeDrawers()
    }

    fun goToGitHubUserPage(userName: String): Boolean {
        WebViewUtil.loadHtml(webView, LinkUtil.getFullFormUrl("https://github.com/" + userName),
                emptyMap(), true, false) {
            Log.d(TAG, "Loaded page from: ${webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (!artifactExists(intent)) {
                etName.setText(userName)
                supportActionBar!!.subtitle = webView.title
            }
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
