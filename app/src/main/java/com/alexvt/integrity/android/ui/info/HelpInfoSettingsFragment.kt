/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.info

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alexvt.integrity.R
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class HelpInfoSettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[HelpInfoViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_info_help, rootKey)

        val prefProject: Preference = findPreference("help_project")
        val prefRestore: Preference = findPreference("help_restore")
        val prefSettings: Preference = findPreference("help_settings")
        val prefLegal: Preference = findPreference("help_legal")
        val prefVersion: Preference = findPreference("help_version")

        prefProject.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            vm.viewProjectLink()
            true
        }
        prefRestore.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            vm.viewRecovery()
            true
        }
        prefSettings.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            vm.viewSettings()
            true
        }
        prefLegal.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            vm.viewLegal()
            true
        }
        prefVersion.summary = "Version ${vm.versionName}"
    }
}