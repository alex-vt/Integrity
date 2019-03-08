/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore

class NotificationSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_notifications, rootKey)

        bindShowErrors()
        bindShowForDisabledScheduledJobs()
    }

    private fun bindShowErrors() {
        val prefShowErrors: SwitchPreferenceCompat = findPreference("notifications_errors_enable")
        updateShowErrors(prefShowErrors)
        prefShowErrors.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val showErrors = IntegrityCore.settingsRepository.get().notificationShowErrors
            IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository.get()
                    .copy(notificationShowErrors = !showErrors))
            updateShowErrors(prefShowErrors)
            true
        }
    }

    private fun updateShowErrors(prefShowErrors: SwitchPreferenceCompat) {
        prefShowErrors.isChecked = IntegrityCore.settingsRepository.get().notificationShowErrors
    }

    private fun bindShowForDisabledScheduledJobs() {
        val prefShowForDisabledScheduled: SwitchPreferenceCompat
                = findPreference("notifications_scheduled_jobs_disabled")
        updateShowForDisabledScheduledJobs(prefShowForDisabledScheduled)
        prefShowForDisabledScheduled.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val show = IntegrityCore.settingsRepository.get().notificationShowDisabledScheduled
            IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository.get()
                    .copy(notificationShowDisabledScheduled = !show))
            updateShowForDisabledScheduledJobs(prefShowForDisabledScheduled)
            true
        }
    }

    private fun updateShowForDisabledScheduledJobs(
            prefDisabledScheduledJobs: SwitchPreferenceCompat) {
        prefDisabledScheduledJobs.isChecked = IntegrityCore.settingsRepository.get()
                .notificationShowDisabledScheduled
    }
}