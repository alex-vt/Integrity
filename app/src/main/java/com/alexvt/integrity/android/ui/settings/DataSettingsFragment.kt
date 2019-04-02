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

class DataSettingsFragment : SettingsFragment() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_data, rootKey)

        bindDownloadLocation()
        bindDestinations()
        bindRecovery()
        bindLegalInfo()
    }

    private fun bindDownloadLocation() = bindTextSetting(
            key = "data_downloads_location",
            title = "Snapshots downloads folder path",
            hint = "Enter path on device storage",
            visibleSettingSelector = { it.dataFolderPath.removePrefix(vm.pathPrefix) },
            visibleSelectionAction = { vm.saveDataFolderPath(it) }
    )

    private fun bindRecovery() = bindSimpleActionSetting(
            key = "data_manage_clear",
            action = { vm.viewRecovery() }
    )

    private fun bindDestinations() = bindSimpleActionSetting(
            key = "data_manage_archives",
            action = { vm.viewDestinations() }
    )

    private fun bindLegalInfo() = bindSimpleActionSetting(
            key = "data_legal",
            action = { vm.viewLegal() }
    )
}