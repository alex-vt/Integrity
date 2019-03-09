/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations.samba

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocation
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocationCredentials
import com.alexvt.integrity.lib.util.ThemedActivity
import kotlinx.android.synthetic.main.activity_samba_location.*


class SambaDestinationActivity : ThemedActivity() {

    private val TAG = SambaDestinationActivity::class.java.simpleName

    // folder location to view/edit
    private lateinit var folderLocation: SambaFolderLocation
    private lateinit var folderLocationCredentials: SambaFolderLocationCredentials

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_samba_location)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        if (editMode(intent)) {
            folderLocation = IntegrityCore.settingsRepository.getAllFolderLocations()
                    .first { it.title == getTitleFromIntent(intent) } as SambaFolderLocation
            folderLocationCredentials = IntegrityCore.credentialsRepository
                    .getCredentials(folderLocation.title) as SambaFolderLocationCredentials

        } else {
            folderLocation = SambaFolderLocation(
                    title = etTitle.text.toString().trim(),
                    fullPath = etPath.text.toString().trim()
            )
            folderLocationCredentials = SambaFolderLocationCredentials(
                    title = etTitle.text.toString().trim(),
                    user = etUser.text.toString().trim(),
                    password = etPassword.text.toString().trim()
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
        etPath.setText(folderLocation.fullPath)
        etUser.setText(folderLocationCredentials.user)
        etPassword.setText("") // user should enter password again
        bSave.setOnClickListener { checkDataAndSave() }
    }

    fun checkDataAndSave() {
        folderLocation = folderLocation.copy(
                title = etTitle.text.toString().trim(),
                fullPath = etPath.text.toString().trim()
        )
        folderLocationCredentials = folderLocationCredentials.copy(
                title = etTitle.text.toString().trim(),
                user = etUser.text.toString().trim(),
                password = etPassword.text.toString().trim()
        )
        if (folderLocation.title.isEmpty()) {
            Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
            return
        }
        if (!folderLocation.fullPath.startsWith("smb://")) {
            Toast.makeText(this, "Please enter Samba folder path", Toast.LENGTH_SHORT).show()
            return
        }
        if (folderLocationCredentials.user.isEmpty()) {
            Toast.makeText(this, "Please enter user name", Toast.LENGTH_SHORT).show()
            return
        }
        if (folderLocationCredentials.password.isEmpty()) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
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
        IntegrityCore.credentialsRepository.removeCredentials(folderLocation.title)
        IntegrityCore.settingsRepository.addFolderLocation(folderLocation)
        IntegrityCore.credentialsRepository.addCredentials(folderLocationCredentials)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}