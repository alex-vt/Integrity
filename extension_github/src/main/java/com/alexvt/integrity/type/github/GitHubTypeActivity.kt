/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.alexvt.integrity.type.github.databinding.GitHubTypeContentBinding
import com.alexvt.integrity.type.github.databinding.GitHubTypeControlsBinding
import com.alexvt.integrity.type.github.databinding.GitHubTypeFilterBinding
import com.alexvt.integrity.lib.DataTypeActivity
import com.alexvt.integrity.lib.util.LinkUtil
import com.alexvt.integrity.lib.util.WebArchiveFilesUtil
import com.alexvt.integrity.lib.util.WebPageLoader
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.SnapshotMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class GitHubTypeActivity : DataTypeActivity() {

    private val TAG = GitHubTypeActivity::class.java.simpleName

    private lateinit var content : GitHubTypeContentBinding
    private lateinit var controls : GitHubTypeControlsBinding
    private lateinit var filter : GitHubTypeFilterBinding


    // Type implementation

    override fun getTypeName() = "GitHub"

    override fun getTypeMetadataNewInstance() = GitHubTypeMetadata()

    override fun inflateContentView(context: Context): ViewDataBinding {
        content = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.git_hub_type_content, null, false)
        return content
    }

    override fun inflateControlsView(context: Context): ViewDataBinding {
        controls = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.git_hub_type_controls, null, false)
        return controls
    }

    override fun inflateFilterView(context: Context): ViewDataBinding {
        filter = DataBindingUtil.inflate(LayoutInflater.from(context),
                R.layout.git_hub_type_filter, null, false)
        return filter
    }

    override fun fillInTypeOptions(snapshot: SnapshotMetadata, isEditable: Boolean) {
        controls.etUserName.isEnabled = isEditable
        controls.etUserName.setText(LinkUtil.getShortFormUrl(getUserName(snapshot)))
        controls.etUserName.setOnEditorActionListener {
            _, _, _ -> goToGitHubUserPage(snapshot, controls.etUserName.text.toString())
        }
        controls.bGo.isEnabled = isEditable
        controls.bGo.setOnClickListener {
            _ -> goToGitHubUserPage(snapshot, controls.etUserName.text.toString())
        }
    }

    /**
     * Performs existing snapshot data loading for viewing.
     *
     * Side effects: reads snapshot data, modifies bound views.
     */
    override fun snapshotViewModeAction(snapshot: SnapshotMetadata) {
        GlobalScope.launch (Dispatchers.Main) {
            content.webView.stopLoading()
            val snapshotPath = IntegrityEx.getSnapshotDataFolderPath(applicationContext,
                    snapshot.artifactId, snapshot.date)
            val linkToArchivePathRedirectMap = WebArchiveFilesUtil
                    .getPageIndexLinkToArchivePathMap(applicationContext, snapshotPath,
                            "file://$snapshotPath/")
            val firstArchivePath = linkToArchivePathRedirectMap.entries.firstOrNull()?.value
                    ?: "file:blank" // todo replace
            WebPageLoader().loadHtml(content.webView,
                    firstArchivePath,
                    linkToArchivePathRedirectMap,
                    true,
                    false) {
                android.util.Log.v(this@GitHubTypeActivity.TAG, "Loaded HTML from file")
                endPreview()
            }
        }
    }

    /**
     * Loads snapshot data preview from the remote source. Before that, changes options when needed.
     *
     * Side effects: shows toast, loads data from source, modifies bound views.
     */
    override fun snapshotCreateModeAction(snapshot: SnapshotMetadata): SnapshotMetadata {
        goToGitHubUserPage(snapshot, controls.etUserName.text.toString())
        return snapshot
    }

    override fun contentCanGoBack() = content.webView.canGoBack()

    override fun contentGoBack() = content.webView.goBack()

    override fun checkSnapshot(snapshot: SnapshotMetadata): Pair<SnapshotMetadata, Boolean> {
        if (controls.etUserName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter user name first", Toast.LENGTH_SHORT).show()
            return Pair(snapshot, false)
        }
        return Pair(snapshot.copy(
                dataTypeSpecificMetadata = GitHubTypeMetadata(
                        userName = if (isSnapshotViewMode()) {
                            getTypeMetadata(snapshot).userName // for read only snapshot, same as it was
                        } else {
                            controls.etUserName.text.toString().trim('/', ' ')
                        }
                )
        ), true)
    }


    // Helpers

    private var loadedHtml = ""

    private fun getTypeMetadata(snapshot: SnapshotMetadata): GitHubTypeMetadata
            = snapshot.dataTypeSpecificMetadata as GitHubTypeMetadata

    private fun getUserName(snapshot: SnapshotMetadata) = getTypeMetadata(snapshot).userName

    private fun goToGitHubUserPage(snapshot: SnapshotMetadata, userName: String): Boolean {
        WebPageLoader().loadHtml(content.webView, LinkUtil.getFullFormUrl("https://github.com/" + userName),
                emptyMap(), true, false) {
            android.util.Log.v(this.TAG, "Loaded page from: ${content.webView.url}")
            loadedHtml = it
            // Inputs are pre-filled only when creating new artifact
            if (isArtifactCreateMode()) {
                setTitleInControls(content.webView.title)
                supportActionBar!!.subtitle = content.webView.title
            }
        }
        return false
    }
}
