/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.alexvt.integrity.R
import com.alexvt.integrity.ui.destinations.DestinationsActivity
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.ui.info.LegalInfoActivity
import com.alexvt.integrity.lib.log.Log
import com.alexvt.integrity.ui.recovery.RecoveryActivity

class DataSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_data, rootKey)

        val prefDownloadsLocation: Preference = findPreference("data_downloads_location")
        val prefClear: Preference = findPreference("data_manage_clear")
        val prefArchives: Preference = findPreference("data_manage_archives")
        val prefLegal: Preference = findPreference("data_legal")

        showDownloadLocationSummary(prefDownloadsLocation)
        prefDownloadsLocation.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // todo path picker, change/move confirmation
            MaterialDialog(context!!).show {
                title(text = "Snapshots downloads folder path")
                input(hint = "Enter path on device storage",
                        prefill = IntegrityCore.getDataFolderName()) { _, text ->
                    val oldFolderName = IntegrityCore.getDataFolderName()
                    val newFolderName = text.trim().toString()
                    IntegrityCore.dataFolderManager.moveDataCacheFolder(oldFolderName, newFolderName)
                    IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository.get()
                            .copy(dataFolderPath = newFolderName))
                    Log(context, "Moved data downloads folder from $oldFolderName to $newFolderName")
                            .log()
                    showDownloadLocationSummary(prefDownloadsLocation)
                }
                positiveButton(text = "Change")
            }
            true
        }

        prefClear.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context!!, RecoveryActivity::class.java))
            true
        }
        prefArchives.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context!!, DestinationsActivity::class.java))
            true
        }
        prefLegal.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context!!, LegalInfoActivity::class.java))
            true
        }
    }

    private fun showDownloadLocationSummary(prefDownloadsLocation: Preference) {
        prefDownloadsLocation.summary = "\uD83D\uDCF1/${IntegrityCore.getDataFolderName()}"
    }
}