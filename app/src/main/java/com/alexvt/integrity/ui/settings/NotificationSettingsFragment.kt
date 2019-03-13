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
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class NotificationSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var integrityCore: IntegrityCore

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_notifications, rootKey)

        bindShowErrors()
        bindShowForDisabledScheduledJobs()
    }

    private fun bindShowErrors() {
        val prefShowErrors: SwitchPreferenceCompat = findPreference("notifications_errors_enable")
        updateShowErrors(prefShowErrors)
        prefShowErrors.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val showErrors = integrityCore.settingsRepository.get().notificationShowErrors
            integrityCore.settingsRepository.set(integrityCore.settingsRepository.get()
                    .copy(notificationShowErrors = !showErrors))
            updateShowErrors(prefShowErrors)
            true
        }
    }

    private fun updateShowErrors(prefShowErrors: SwitchPreferenceCompat) {
        prefShowErrors.isChecked = integrityCore.settingsRepository.get().notificationShowErrors
    }

    private fun bindShowForDisabledScheduledJobs() {
        val prefShowForDisabledScheduled: SwitchPreferenceCompat
                = findPreference("notifications_scheduled_jobs_disabled")
        updateShowForDisabledScheduledJobs(prefShowForDisabledScheduled)
        prefShowForDisabledScheduled.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val show = integrityCore.settingsRepository.get().notificationShowDisabledScheduled
            integrityCore.settingsRepository.set(integrityCore.settingsRepository.get()
                    .copy(notificationShowDisabledScheduled = !show))
            updateShowForDisabledScheduledJobs(prefShowForDisabledScheduled)
            true
        }
    }

    private fun updateShowForDisabledScheduledJobs(
            prefDisabledScheduledJobs: SwitchPreferenceCompat) {
        prefDisabledScheduledJobs.isChecked = integrityCore.settingsRepository.get()
                .notificationShowDisabledScheduled
    }
}