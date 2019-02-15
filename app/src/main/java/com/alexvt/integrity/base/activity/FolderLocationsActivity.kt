/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.base.adapter.FolderLocationRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.util.IntentUtil
import com.leinardi.android.speeddial.SpeedDialActionItem
import kotlinx.android.synthetic.main.activity_folder_locations.*

class FolderLocationsActivity : AppCompatActivity() {

    var selectedFolderLocations: List<FolderLocation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_locations)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        if (IntentUtil.getSnapshot(intent) != null) {
            selectedFolderLocations = IntentUtil.getSnapshot(intent)!!.archiveFolderLocations
        }

        // Float Action Button action items for each available folder location type
        // Folder location type map is sorted by key, so the value will be obtained by index of clicked action
        IntegrityCore.getNamedFileLocationCreateIntentMap().keys
                .forEachIndexed { index, key ->
                    sdAdd.addActionItem(SpeedDialActionItem.Builder(index, android.R.drawable.ic_input_add)
                            .setLabel(key)
                            .create())
                }
        sdAdd.setOnActionSelectedListener { speedDialActionItem ->
            val typeViewIntent = IntegrityCore.getNamedFileLocationCreateIntentMap().values
                    .toList()[speedDialActionItem.id]
            startActivity(typeViewIntent)
            false
        }
        rvFolderLocationList.adapter = FolderLocationRecyclerAdapter(ArrayList(), this)

        bDone.visibility = if (isSelectMode()) View.VISIBLE else View.GONE
        bDone.setOnClickListener { returnSelection() }
    }

    override fun onStart() {
        super.onStart()
        refreshFolderLocationList() // no other sources of data, no need to listen to changes
    }


    private fun refreshFolderLocationList() {
        (rvFolderLocationList.adapter as FolderLocationRecyclerAdapter)
                .setItems(getItemSelection())
    }

    private fun getItemSelection()
            = IntegrityCore.folderLocationRepository.getAllFolderLocations().map {
                Pair(it, selectedFolderLocations.contains(it))
            }

    fun viewFolderLocation(title: String) {
        startActivity(IntegrityCore.getFolderLocationEditIntent(title))
    }

    fun toggleSelection(folderLocation: FolderLocation) {
        if (selectedFolderLocations.contains(folderLocation)) {
            selectedFolderLocations = selectedFolderLocations.minus(folderLocation)
        } else {
            selectedFolderLocations = selectedFolderLocations.plus(folderLocation)
        }
        refreshFolderLocationList()
    }

    fun askRemoveFolderLocation(title: String) {
        MaterialDialog(this)
                .title(text = "Delete folder location?\nSome artifacts may fail to save there.")
                .positiveButton(text = "Delete") {
                    dialog ->
                    IntegrityCore.folderLocationRepository.removeFolderLocationAndCredentials(title)
                    refreshFolderLocationList()
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    fun isSelectMode() = IntentUtil.isSelectMode(intent)

    private fun returnSelection() {
        val returnIntent = Intent()
        if (IntentUtil.getSnapshot(intent) != null) {
            val snapshot = IntentUtil.getSnapshot(intent)!!.copy(
                    archiveFolderLocations = ArrayList(selectedFolderLocations)
            )
            IntentUtil.putSnapshot(returnIntent, snapshot)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
