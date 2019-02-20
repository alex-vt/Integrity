/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.settings

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore

class BehaviorSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_behavior, rootKey)

        // todo replace error prone implementation
        bindExpandRunning()
        bindExpandScheduled()
    }

    private fun bindExpandRunning() {
        val prefExpandRunning: CheckBoxPreference = findPreference("behavior_drawer_expand_running")
        updateExpandRunning(prefExpandRunning)
        prefExpandRunning.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val expandRunning = IntegrityCore.settingsRepository.get().menuExpandJobsRunning
            IntegrityCore.settingsRepository.set(context!!, IntegrityCore.settingsRepository.get()
                    .copy(menuExpandJobsRunning = !expandRunning))
            updateExpandRunning(prefExpandRunning)
            true
        }
    }

    private fun bindExpandScheduled() {
        val prefExpandScheduled: CheckBoxPreference = findPreference("behavior_drawer_expand_scheduled")
        updateExpandScheduled(prefExpandScheduled)
        prefExpandScheduled.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val expandScheduled = IntegrityCore.settingsRepository.get().menuExpandJobsScheduled
            IntegrityCore.settingsRepository.set(context!!, IntegrityCore.settingsRepository.get()
                    .copy(menuExpandJobsScheduled = !expandScheduled))
            updateExpandScheduled(prefExpandScheduled)
            true
        }
    }

    private fun updateExpandScheduled(prefExpandScheduled: CheckBoxPreference) {
        prefExpandScheduled.isChecked = IntegrityCore.settingsRepository.get()
                .menuExpandJobsScheduled
    }

    private fun updateExpandRunning(prefExpandRunning: CheckBoxPreference) {
        prefExpandRunning.isChecked = IntegrityCore.settingsRepository.get()
                .menuExpandJobsRunning
    }
}