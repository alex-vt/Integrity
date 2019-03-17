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
import com.afollestad.materialdialogs.MaterialDialog
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class ExtensionSettingsFragment : SettingsFragment() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_extensions, rootKey)

        bindGetExtensions()
        bindNonIncludedExtensions()
    }

    private fun bindGetExtensions() = bindSimpleActionSetting(
            key = "extensions_get",
            action = {
                MaterialDialog(context!!).show {
                    title(text = "Extensions for more data types")
                    message(text = "Integrity app extensions are separate apps. Install them and " +
                            "new data types will become available for creating snapshots.")
                    positiveButton(text = "OK")
                }
            }
    )

    private fun bindNonIncludedExtensions() {
        vm.getNonIncludedExtensions().forEach {
            addSimpleActionSetting(
                    categoryKey = "extensions",
                    title = "${it.title} type",
                    summary = "View app info",
                    action = { vm.viewAppInfo(it.packageName) }
            )
        }
    }
}