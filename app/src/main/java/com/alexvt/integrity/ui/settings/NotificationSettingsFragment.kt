/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.alexvt.integrity.R
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class NotificationSettingsFragment : SettingsFragment() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_notifications, rootKey)

        bindShowErrors()
        bindShowForDisabledScheduledJobs()
    }

    private fun bindShowErrors() = bindToggleSetting(
            key = "notifications_errors_enable",
            settingSelector = { it.notificationShowErrors },
            toggleAction = { vm.toggleErrorNotificationsEnabled() }
    )

    private fun bindShowForDisabledScheduledJobs() = bindToggleSetting(
            key = "notifications_scheduled_jobs_disabled",
            settingSelector = { it.notificationShowDisabledScheduled },
            toggleAction = { vm.toggleScheduledJobsDisabledNotification() }
    )
}