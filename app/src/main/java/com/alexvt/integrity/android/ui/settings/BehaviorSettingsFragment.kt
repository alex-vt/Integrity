/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.settings

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.alexvt.integrity.R
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class BehaviorSettingsFragment : SettingsFragment() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_behavior, rootKey)

        bindEnableScheduled()
        bindExpandRunning()
        bindExpandScheduled()
        bindSorting()
        bindFasterFiltering()
    }

    private fun bindEnableScheduled() = bindToggleSetting(
            key = "behavior_jobs_enable_scheduled",
            settingSelector = { it.jobsEnableScheduled },
            toggleAction = { vm.toggleScheduledJobsEnabled() }
    )

    private fun bindExpandRunning() = bindToggleSetting(
            key = "behavior_jobs_expand_running",
            settingSelector = { it.jobsExpandRunning },
            toggleAction = { vm.toggleRunningJobsExpand() }
    )

    private fun bindExpandScheduled() = bindToggleSetting(
            key = "behavior_jobs_expand_scheduled",
            settingSelector = { it.jobsExpandScheduled },
            toggleAction = { vm.toggleScheduledJobsExpand() }
    )

    private fun bindSorting() = bindSelectionSetting(
            key = "behavior_filtering_sorting",
            title = "Sorting method",
            items = vm.getAllSortingMethods(),
            initialSelection = vm.getCurrentSortingMethodIndex(),
            settingSelector = { it.sortingMethod },
            selectionAction = { vm.saveSortingMethod(it) }
    )

    private fun bindFasterFiltering() = bindToggleSetting(
            key = "behavior_filtering_faster",
            settingSelector = { it.fasterSearchInputs },
            toggleAction = { vm.toggleSearchFaster() }
    )
}