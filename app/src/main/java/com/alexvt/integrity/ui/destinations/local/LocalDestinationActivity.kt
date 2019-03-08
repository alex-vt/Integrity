/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations.local

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.destinations.local.LocalFolderLocation
import com.alexvt.integrity.lib.util.ThemedActivity
import kotlinx.android.synthetic.main.activity_local_location.*


class LocalDestinationActivity : ThemedActivity() {

    private val TAG = LocalDestinationActivity::class.java.simpleName

    // folder location to view/edit
    private lateinit var folderLocation: LocalFolderLocation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_location)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        if (editMode(intent)) {
            folderLocation = IntegrityCore.settingsRepository.getAllFolderLocations()
                    .first { it.title == getTitleFromIntent(intent) } as LocalFolderLocation

        } else {
            folderLocation = LocalFolderLocation(
                    title = etTitle.text.toString().trim(),
                    folderPath = etPath.text.toString().trim()
            )
        }

        fillInOptions(editMode(intent))
    }

    fun getTitleFromIntent(intent: Intent?): String {
        var title: String? = intent?.getStringExtra("title")
        if (title == null) {
            title = ""
        }
        return title
    }

    fun editMode(intent: Intent?) = getTitleFromIntent(intent).isNotEmpty()

    fun fillInOptions(editing: Boolean) {
        etTitle.setText(folderLocation.title)
        etTitle.isEnabled = !editing
        etPath.setText(folderLocation.folderPath)
        bSave.setOnClickListener { checkDataAndSave() }
    }


    fun checkDataAndSave() {
        folderLocation = folderLocation.copy(
                title = etTitle.text.toString().trim(),
                folderPath = etPath.text.toString().trim()
        )
        if (folderLocation.title.isEmpty()) {
            Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
            return
        }
        if (!folderLocation.folderPath.startsWith("/")) {
            Toast.makeText(this, "Please enter local folder path", Toast.LENGTH_SHORT).show()
            return
        }
        // when creating new location, it must have unique title
        val titleAlreadyExists = IntegrityCore.settingsRepository.getAllFolderLocations()
                .any { it.title == folderLocation.title }
        if (!editMode(intent) && titleAlreadyExists) {
            Toast.makeText(this, "Location with this title already exists",
                    Toast.LENGTH_SHORT).show()
            return
        }
        // the old one is removed first
        IntegrityCore.settingsRepository.removeFolderLocation(folderLocation.title)
        IntegrityCore.settingsRepository.addFolderLocation(folderLocation)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
