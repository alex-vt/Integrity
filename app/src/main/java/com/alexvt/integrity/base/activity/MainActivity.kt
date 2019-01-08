/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.base.adapter.ArtifactRecyclerAdapter
import com.alexvt.integrity.base.adapter.JobRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.IntentUtil
import com.leinardi.android.speeddial.SpeedDialActionItem

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        setupDrawerToggle()
        toolbar.title = "Artifacts"

        // Float Action Button action items for each available data type
        // Data type map is sorted by key, so the value will be obtained by index of clicked action
        IntegrityCore.getTypeNames()
                .forEachIndexed { index, component ->
            sdAdd.addActionItem(SpeedDialActionItem.Builder(index, android.R.drawable.ic_input_add)
                    .setLabel(component.className.substringAfterLast(".")
                            .removeSuffix("TypeActivity")) // todo names from resources
                    .create())
        }
        sdAdd.setOnActionSelectedListener { speedDialActionItem ->
            val typeName = IntegrityCore.getTypeNames().toList()[speedDialActionItem.id]
            IntegrityCore.openCreateNewArtifact(this, typeName)
            false
        }

        rvArtifactList.adapter = ArtifactRecyclerAdapter(ArrayList(), this)
        rvJobs.adapter = JobRecyclerAdapter(ArrayList(), this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snapshot = IntentUtil.getSnapshot(data)
        if (snapshot != null) {
            IntegrityCore.saveSnapshot(this, snapshot)
        }
    }

    fun setupDrawerToggle() {
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        val drawerToggle = ActionBarDrawerToggle(this, dlAllContent, toolbar, 0, 0)
        dlAllContent.setDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    override fun onStart() {
        super.onStart()
        IntegrityCore.metadataRepository.addChangesListener(MainActivity::class.java.simpleName) {
            refreshArtifactList()
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
                    dialog ->
                    IntegrityCore.removeArtifact(artifactId, false)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    fun askRemoveAll() {
        MaterialDialog(this)
                .title(text = "Delete all artifacts?")
                .positiveButton(text = "Delete") {
                    dialog ->
                    IntegrityCore.removeAll(false)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    private fun refreshArtifactList() {
        (rvArtifactList.adapter as ArtifactRecyclerAdapter)
                .setItems(IntegrityCore.metadataRepository.getAllArtifactLatestMetadata(true)
                        .snapshots.toList())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_folder_locations -> {
                viewFolderLocations()
                true
            }
            R.id.action_delete_all -> {
                askRemoveAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun viewFolderLocations() {
        startActivity(Intent(this, FolderLocationsActivity::class.java))
    }

    fun viewArtifact(artifactId: Long) {
        val intent = Intent(this, ArtifactViewActivity::class.java)
        intent.putExtra("artifactId", artifactId)
        startActivity(intent)
    }
}
