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

class BehaviorSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
            val enableScheduled = IntegrityCore.scheduledJobsEnabled()
            IntegrityCore.updateScheduledJobsOptions(!enableScheduled)
            updateEnableScheduled(prefEnableScheduled)
            true
        }
    }

    private fun bindExpandRunning() {
        val prefExpandRunning: CheckBoxPreference = findPreference("behavior_jobs_expand_running")
        updateExpandRunning(prefExpandRunning)
        prefExpandRunning.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val expandRunning = IntegrityCore.settingsRepository.get().jobsExpandRunning
            IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository.get()
                    .copy(jobsExpandRunning = !expandRunning))
            updateExpandRunning(prefExpandRunning)
            true
        }
    }

    private fun bindExpandScheduled() {
        val prefExpandScheduled: CheckBoxPreference = findPreference("behavior_jobs_expand_scheduled")
        updateExpandScheduled(prefExpandScheduled)
        prefExpandScheduled.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val expandScheduled = IntegrityCore.settingsRepository.get().jobsExpandScheduled
            IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository.get()
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
                                .indexOf(IntegrityCore.getSortingMethod())
                ) { _, index, _ ->
                    val sortingMethod = SortingUtil.getSortingMethodNameMap().keys.toList()[index]
                    IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository
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
            val fasterFiltering = IntegrityCore.settingsRepository.get().fasterSearchInputs
            IntegrityCore.settingsRepository.set(IntegrityCore.settingsRepository.get()
                    .copy(fasterSearchInputs = !fasterFiltering))
            updateFasterFiltering(prefFasterFiltering)
            true
        }
    }

    private fun updateEnableScheduled(prefEnableScheduled: SwitchPreferenceCompat) {
        prefEnableScheduled.isChecked = IntegrityCore.scheduledJobsEnabled()
    }

    private fun updateExpandScheduled(prefExpandScheduled: CheckBoxPreference) {
        prefExpandScheduled.isChecked = IntegrityCore.settingsRepository.get()
                .jobsExpandScheduled
    }

    private fun updateExpandRunning(prefExpandRunning: CheckBoxPreference) {
        prefExpandRunning.isChecked = IntegrityCore.settingsRepository.get()
                .jobsExpandRunning
    }

    private fun updateSorting(prefSorting: Preference) {
        prefSorting.summary = SortingUtil.getSortingMethodNameMap()[IntegrityCore
                .settingsRepository.get().sortingMethod]
    }

    private fun updateFasterFiltering(prefFasterFiltering: CheckBoxPreference) {
        prefFasterFiltering.isChecked = IntegrityCore.settingsRepository.get()
                .fasterSearchInputs
    }
}