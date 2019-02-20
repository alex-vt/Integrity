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
import androidx.preference.SwitchPreferenceCompat
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore

class NotificationSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_notifications, rootKey)

        bindShowErrors()
    }

    private fun bindShowErrors() {
        val prefShowErrors: SwitchPreferenceCompat = findPreference("notifications_errors_enable")
        updateShowErrors(prefShowErrors)
        prefShowErrors.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val showErrors = IntegrityCore.settingsRepository.get().notificationShowErrors
            IntegrityCore.settingsRepository.set(context!!, IntegrityCore.settingsRepository.get()
                    .copy(notificationShowErrors = !showErrors))
            updateShowErrors(prefShowErrors)
            IntegrityCore.notifyAboutUnreadErrors(context!!)
            true
        }
    }

    private fun updateShowErrors(prefShowErrors: SwitchPreferenceCompat) {
        prefShowErrors.isChecked = IntegrityCore.settingsRepository.get().notificationShowErrors
    }
}