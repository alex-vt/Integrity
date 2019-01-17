/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import android.util.Log
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
import kotlinx.android.synthetic.main.bottom_controls_common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class GitHubTypeActivity : DataTypeActivity() {

    private val TAG = GitHubTypeActivity::class.java.simpleName

    override fun initData() {
        if (isSnapshotViewMode()) {
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

        } else if (isSnapshotCreateMode()) {
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

    override fun checkSnapshot(status: String): Boolean {
        if (etUserName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter uer name first", Toast.LENGTH_SHORT).show()
            return false
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
                        userName = if (isSnapshotViewMode()) {
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

    override fun getDrawer() = dlAllContent


    // type specific

    fun fillInOptions(isEditable: Boolean) {
        fillInCommonOptions(isEditable)

        etUserName.isEnabled = isEditable
        etUserName.setText(LinkUtil.getShortFormUrl(getUserName()))
        etUserName.setOnEditorActionListener { v, actionId, event -> goToGitHubUserPage(etUserName.text.toString()) }
        bGo.isEnabled = isEditable
        bGo.setOnClickListener { view -> goToGitHubUserPage(etUserName.text.toString()) }
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
            if (isArtifactCreateMode()) {
                etName.setText(userName)
                supportActionBar!!.subtitle = webView.title
            }
        }
        return false
    }

}
