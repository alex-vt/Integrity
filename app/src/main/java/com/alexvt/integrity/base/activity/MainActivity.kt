/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.base.adapter.ArtifactRecyclerAdapter
import com.alexvt.integrity.base.adapter.SearchResultRecyclerAdapter
import com.alexvt.integrity.base.adapter.JobRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.search.SearchUtil
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.util.IntentUtil
import com.jakewharton.rxbinding3.widget.textChanges
import com.leinardi.android.speeddial.SpeedDialActionItem
import io.reactivex.android.schedulers.AndroidSchedulers

import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import android.view.*
import com.alexvt.integrity.util.SpeedDialCompatUtil


class MainActivity : AppCompatActivity() {

    data class Inputs(val filteredArtifactId: Long?, val searchText: String)

    private var inputs: Inputs = Inputs(null, "")

    private fun onInputsUpdate(newInputs: Inputs) {
        inputs = newInputs
        refreshSnapshotList(inputs.filteredArtifactId)
        search(inputs.searchText, inputs.filteredArtifactId)
        updateFilterView(inputs.filteredArtifactId)
        updateAddButton(inputs.filteredArtifactId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        bindDrawer()
        bindAddButton()
        bindSnapshotList()
        bindFilter()
        bindSearch()

        onInputsUpdate(inputs)
    }

    private fun bindDrawer() {
        // toggling
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        val drawerToggle = ActionBarDrawerToggle(this, dlAllContent, toolbar, 0, 0)
        dlAllContent.setDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // content
        rvJobs.adapter = JobRecyclerAdapter(ArrayList(), this)
        bLog.setOnClickListener { viewLog() }
    }

    private fun bindSnapshotList() {
        rvSnapshotList.adapter = ArtifactRecyclerAdapter(ArrayList(), this)
    }

    private fun bindAddButton() {
        SpeedDialCompatUtil.setStayOnExpand(sdAdd)
    }

    private fun updateAddButton(artifactId: Long?) {
        sdAdd.clearActionItems()
        if (artifactId != null) {
            sdAdd.addActionItem(SpeedDialActionItem.Builder(0, android.R.drawable.ic_input_add)
                    .setLabel("Create another snapshot")
                    .create())
            sdAdd.setOnActionSelectedListener { speedDialActionItem ->
                addSnapshot(artifactId)
                false
            }
        } else {
            IntegrityCore.getTypeNames().map {
                // todo names from resources
                it.className.substringAfterLast(".").removeSuffix("TypeActivity")
            }.forEachIndexed {
                index, name -> sdAdd.addActionItem(SpeedDialActionItem
                    .Builder(index, android.R.drawable.ic_input_add)
                    .setLabel(name)
                    .create())
            }
            sdAdd.setOnActionSelectedListener { speedDialActionItem ->
                val typeName = IntegrityCore.getTypeNames().toList()[speedDialActionItem.id]
                IntegrityCore.openCreateNewArtifact(this, typeName)
                false
            }
        }
    }

    private fun bindFilter() {
        ivUnFilterArtifact.setOnClickListener { removeArtifactFilter() }
    }

    private fun updateFilterView(artifactId: Long?) {
        if (artifactId != null) {
            tvFilteredArtifactTitle.text = IntegrityCore.metadataRepository
                    .getLatestSnapshotMetadata(artifactId).title
        }
        llFilteredArtifact.visibility = if (artifactId != null) View.VISIBLE else View.GONE
    }

    private fun bindSearch() {
        rvSearchResults.adapter = SearchResultRecyclerAdapter(ArrayList(), this)
        etSearch.textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onInputsUpdate(inputs.copy(searchText = it.trim().toString())) }
    }

    private fun search(searchedText: String, filteredArtifactId: Long?) {
        if (searchedText.isBlank()) {
            toolbar.title = "Snapshots"
            toolbar.subtitle = when {
                filteredArtifactId != null -> "of ${IntegrityCore.metadataRepository
                        .getLatestSnapshotMetadata(filteredArtifactId).title}"
                else -> "Recent in all"
            }
        } else {
            toolbar.title = "Search"
            toolbar.subtitle = when {
                filteredArtifactId != null -> "in ${IntegrityCore.metadataRepository
                        .getLatestSnapshotMetadata(filteredArtifactId).title}"
                else -> "In all"
            }
        }
        if (searchedText.length >= 3) {
            val searchResults = SearchUtil.searchText(searchedText, filteredArtifactId)
            (rvSearchResults.adapter as SearchResultRecyclerAdapter).setItems(searchResults)
            tvNoResults.visibility = if (searchResults.isEmpty()) View.VISIBLE else View.GONE
        } else {
            tvNoResults.visibility = View.GONE
        }
        rvSnapshotList.visibility = if (searchedText.isBlank()) View.VISIBLE else View.GONE
        rvSearchResults.visibility = if (searchedText.isBlank()) View.GONE else View.VISIBLE
        sdAdd.visibility = if (searchedText.isBlank()) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snapshot = IntentUtil.getSnapshot(data)
        if (snapshot != null) {
            IntegrityCore.saveSnapshot(this, snapshot)
        }
    }

    override fun onStart() {
        super.onStart()
        IntegrityCore.metadataRepository.addChangesListener(MainActivity::class.java.simpleName) {
            refreshSnapshotList(inputs.filteredArtifactId)
        }
        IntegrityCore.subscribeToScheduledJobListing(MainActivity::class.java.simpleName) {
            refreshJobList(it, false)
        }
        IntegrityCore.subscribeToRunningJobListing(MainActivity::class.java.simpleName) {
            refreshJobList(it, true)
        }
    }

    override fun onStop() {
        super.onStop()
        IntegrityCore.metadataRepository.removeChangesListener(MainActivity::class.java.simpleName)
        IntegrityCore.unsubscribeFromScheduledJobListing(MainActivity::class.java.simpleName)
        IntegrityCore.unsubscribeFromRunningJobListing(MainActivity::class.java.simpleName)
    }

    private fun refreshJobList(scheduledJobIds: List<Pair<Long, String>>, isRunning: Boolean) {
        (rvJobs.adapter as JobRecyclerAdapter)
                .setItems(scheduledJobIds.map {
                    IntegrityCore.metadataRepository.getSnapshotMetadata(it.first, it.second)
                }, isRunning)
    }

    fun askRemoveArtifact(artifactId: Long) {
        MaterialDialog(this)
                .title(text = "Delete artifact?\nData archives will not be affected.")
                .positiveButton(text = "Delete") {
                    IntegrityCore.removeArtifact(artifactId, false)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    private fun askRemoveAll() {
        MaterialDialog(this)
                .title(text = "Delete all artifacts?")
                .positiveButton(text = "Delete") {
                    IntegrityCore.removeAll(false)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    fun addSnapshot(artifactId: Long) = IntegrityCore.openCreateNewSnapshot(this, artifactId)

    private fun refreshSnapshotList(artifactId: Long?) {
        val snapshots = when (artifactId) {
            null -> IntegrityCore.metadataRepository.getAllArtifactLatestMetadata(true)
            else -> IntegrityCore.metadataRepository.getArtifactMetadata(artifactId)
        }.snapshots
        (rvSnapshotList.adapter as ArtifactRecyclerAdapter)
                .setItems(snapshots.map { Pair(it, getSnapshotCount(it.artifactId)) }.toList(),
                        inputs.filteredArtifactId == null)
    }

    private fun getSnapshotCount(artifactId: Long) = IntegrityCore.metadataRepository
            .getArtifactMetadata(artifactId).snapshots.count()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_folder_locations -> {
            viewFolderLocations()
            true
        }
        R.id.action_tags -> {
            viewTags()
            true
        }
        R.id.action_delete_all -> {
            askRemoveAll()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun viewLog() {
        startActivity(Intent(this, LogViewActivity::class.java))
    }

    private fun viewFolderLocations() {
        startActivity(Intent(this, FolderLocationsActivity::class.java))
    }

    private fun viewTags() {
        startActivity(Intent(this, TagsActivity::class.java))
    }

    fun filterArtifact(artifactId: Long?) {
        onInputsUpdate(inputs.copy(filteredArtifactId = artifactId))
    }

    private fun removeArtifactFilter() = filterArtifact(null)

    fun viewSnapshot(snapshot: Snapshot) {
        IntegrityCore.openViewSnapshotOrShowProgress(this, snapshot.artifactId, snapshot.date)
    }
}
