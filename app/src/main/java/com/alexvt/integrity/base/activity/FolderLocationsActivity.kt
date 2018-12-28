/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.base.adapter.FolderLocationRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import com.leinardi.android.speeddial.SpeedDialActionItem
import kotlinx.android.synthetic.main.activity_folder_locations.*

class FolderLocationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_locations)
        setSupportActionBar(toolbar)

        // Float Action Button action items for each available folder location type
        // Folder location type map is sorted by key, so the value will be obtained by index of clicked action
        IntegrityCore.getNamedFileLocationCreateIntentMap(this).keys
                .forEachIndexed { index, key ->
                    sdAdd.addActionItem(SpeedDialActionItem.Builder(index, android.R.drawable.ic_input_add)
                            .setLabel(key)
                            .create())
                }
        sdAdd.setOnActionSelectedListener { speedDialActionItem ->
            val typeViewIntent = IntegrityCore.getNamedFileLocationCreateIntentMap(this).values
                    .toList()[speedDialActionItem.id]
            startActivity(typeViewIntent)
            false
        }
        rvFolderLocationList.adapter = FolderLocationRecyclerAdapter(ArrayList(), this)
    }

    override fun onStart() {
        super.onStart()
        refreshFolderLocationList()
    }

    private fun refreshFolderLocationList() {
        (rvFolderLocationList.adapter as FolderLocationRecyclerAdapter)
                .setItems(IntegrityCore.folderLocationRepository.getAllFolderLocations())
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

    fun viewFolderLocation(title: String) {
        startActivity(IntegrityCore.getFolderLocationEditIntent(this, title))
    }
}
