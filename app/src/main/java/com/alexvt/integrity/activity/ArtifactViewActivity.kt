/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.alexvt.integrity.R
import com.alexvt.integrity.adapter.SnapshotRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import kotlinx.android.synthetic.main.activity_artifact_view.*

class ArtifactViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artifact_view)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view -> createNewSnapshot(getArtifactIdFromIntent(intent)) }

        rvSnapshotList.adapter = SnapshotRecyclerAdapter(ArrayList(), this)
    }

    override fun onStart() {
        super.onStart()
        IntegrityCore.metadataRepository.addChangesListener(ArtifactViewActivity::class.java.simpleName) {
            refreshSnapshotList()
        }
    }

    override fun onStop() {
        super.onStop()
        IntegrityCore.metadataRepository.removeChangesListener(ArtifactViewActivity::class.java.simpleName)
    }

    fun getArtifactIdFromIntent(intent: Intent?): Long {
        return intent?.getLongExtra("artifactId", -1) ?: -1
    }

    private fun refreshSnapshotList() {
        (rvSnapshotList.adapter as SnapshotRecyclerAdapter).setItems(IntegrityCore
                .metadataRepository.getArtifactMetadata(getArtifactIdFromIntent(intent))
                .snapshotMetadataList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_delete_all -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun previewSnapshot(artifactId: Long, date: String) {
        startActivity(IntegrityCore.getSnapshotViewIntent(this, artifactId, date))
    }

    fun createNewSnapshot(artifactId: Long) {
        startActivity(IntegrityCore.getSnapshotCreateIntent(this, artifactId))
    }
}
