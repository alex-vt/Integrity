/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.database.SimplePersistableMetadataRepository
import kotlinx.android.synthetic.main.activity_snapshot_preview.*

class SnapshotPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snapshot_preview)
        setSupportActionBar(toolbar)

        val artifactId = ArtifactViewActivity.getArtifactIdFromIntent(intent)
        val date = ArtifactViewActivity.getDateFromIntent(intent)
        val snapshot = SimplePersistableMetadataRepository
                .getSnapshotMetadata(artifactId = artifactId, date = date)

        tvTitle.text = snapshot.title
        // todo get screen name from type util
        tvDescription.setText("Type: " + snapshot.dataTypeSpecificMetadata.javaClass.simpleName + "\n"
                + "Description:\n" + snapshot.description)

        // todo show archive locations

        bViewData.setOnClickListener { view -> showSnapshotData(artifactId, date) }
    }

    private fun showSnapshotData(artifactId: Long, date: String) {
        startActivity(IntegrityCore.getSnapshotViewIntent(this, artifactId, date))
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
}
