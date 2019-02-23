/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.info

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alexvt.integrity.BuildConfig
import com.alexvt.integrity.R
import com.alexvt.integrity.core.util.LinkViewUtil
import com.alexvt.integrity.recovery.RecoveryActivity
import com.alexvt.integrity.settings.SettingsActivity

class HelpInfoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_info_help, rootKey)

        val prefProject: Preference = findPreference("help_project")
        val prefRestore: Preference = findPreference("help_restore")
        val prefSettings: Preference = findPreference("help_settings")
        val prefLegal: Preference = findPreference("help_legal")
        val prefVersion: Preference = findPreference("help_version")

        prefProject.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            LinkViewUtil.viewExternal(context!!,
                    "https://github.com/alex-vt/Integrity/tree/develop") // todo update
            true
        }
        prefRestore.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context!!, RecoveryActivity::class.java))
            true
        }
        prefSettings.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context!!, SettingsActivity::class.java))
            true
        }
        prefLegal.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivity(Intent(context!!, LegalInfoActivity::class.java))
            true
        }
        prefVersion.summary = "Version ${BuildConfig.VERSION_NAME}"
    }
}