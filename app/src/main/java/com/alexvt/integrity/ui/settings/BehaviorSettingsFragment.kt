/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.app.Activity
import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.search.SortingUtil
import com.alexvt.integrity.lib.util.IntentUtil
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class BehaviorSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var integrityCore: IntegrityCore

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_behavior, rootKey)

        // todo replace error prone implementation
        bindEnableScheduled()
        bindExpandRunning()
        bindExpandScheduled()
        bindSorting()
        bindFasterFiltering()
    }

    private fun bindEnableScheduled() {
        val prefEnableScheduled: SwitchPreferenceCompat = findPreference("behavior_jobs_enable_scheduled")
        updateEnableScheduled(prefEnableScheduled)
        prefEnableScheduled.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val enableScheduled = integrityCore.scheduledJobsEnabled()
            integrityCore.updateScheduledJobsOptions(!enableScheduled)
            updateEnableScheduled(prefEnableScheduled)
            true
        }
    }

    private fun bindExpandRunning() {
        val prefExpandRunning: CheckBoxPreference = findPreference("behavior_jobs_expand_running")
        updateExpandRunning(prefExpandRunning)
        prefExpandRunning.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val expandRunning = integrityCore.settingsRepository.get().jobsExpandRunning
            integrityCore.settingsRepository.set(integrityCore.settingsRepository.get()
                    .copy(jobsExpandRunning = !expandRunning))
            updateExpandRunning(prefExpandRunning)
            true
        }
    }

    private fun bindExpandScheduled() {
        val prefExpandScheduled: CheckBoxPreference = findPreference("behavior_jobs_expand_scheduled")
        updateExpandScheduled(prefExpandScheduled)
        prefExpandScheduled.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val expandScheduled = integrityCore.settingsRepository.get().jobsExpandScheduled
            integrityCore.settingsRepository.set(integrityCore.settingsRepository.get()
                    .copy(jobsExpandScheduled = !expandScheduled))
            updateExpandScheduled(prefExpandScheduled)
            true
        }
    }

    private fun bindSorting() {
        val prefSorting: Preference = findPreference("behavior_filtering_sorting")
        updateSorting(prefSorting)
        prefSorting.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialDialog(context!!).show {
                title(text = "Sorting method")
                listItemsSingleChoice(
                        items = SortingUtil.getSortingMethodNameMap().values.toList(),
                        initialSelection = SortingUtil.getSortingMethodNameMap().keys.toList()
                                .indexOf(integrityCore.getSortingMethod())
                ) { _, index, _ ->
                    val sortingMethod = SortingUtil.getSortingMethodNameMap().keys.toList()[index]
                    integrityCore.settingsRepository.set(integrityCore.settingsRepository
                            .get().copy(sortingMethod = sortingMethod))
                    updateSorting(prefSorting)
                    activity!!.setResult(Activity.RESULT_OK, IntentUtil.withRefresh(true))
                }
            }
            true
        }
    }

    private fun bindFasterFiltering() {
        val prefFasterFiltering: CheckBoxPreference = findPreference("behavior_filtering_faster")
        updateFasterFiltering(prefFasterFiltering)
        prefFasterFiltering.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val fasterFiltering = integrityCore.settingsRepository.get().fasterSearchInputs
            integrityCore.settingsRepository.set(integrityCore.settingsRepository.get()
                    .copy(fasterSearchInputs = !fasterFiltering))
            updateFasterFiltering(prefFasterFiltering)
            true
        }
    }

    private fun updateEnableScheduled(prefEnableScheduled: SwitchPreferenceCompat) {
        prefEnableScheduled.isChecked = integrityCore.scheduledJobsEnabled()
    }

    private fun updateExpandScheduled(prefExpandScheduled: CheckBoxPreference) {
        prefExpandScheduled.isChecked = integrityCore.settingsRepository.get()
                .jobsExpandScheduled
    }

    private fun updateExpandRunning(prefExpandRunning: CheckBoxPreference) {
        prefExpandRunning.isChecked = integrityCore.settingsRepository.get()
                .jobsExpandRunning
    }

    private fun updateSorting(prefSorting: Preference) {
        prefSorting.summary = SortingUtil.getSortingMethodNameMap()[integrityCore
                .settingsRepository.get().sortingMethod]
    }

    private fun updateFasterFiltering(prefFasterFiltering: CheckBoxPreference) {
        prefFasterFiltering.isChecked = integrityCore.settingsRepository.get()
                .fasterSearchInputs
    }
}