/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.alexvt.integrity.R
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.ViewExternalUtil


class ExtensionSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_extensions, rootKey)

        val prefGetExtensions: Preference = findPreference("extensions_get")
        prefGetExtensions.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // todo GitHub search option
            MaterialDialog(context!!).show {
                title(text = "Extensions for more data types")
                message(text = "Integrity app extensions are separate apps. Install them and " +
                        "new data types will become available for creating snapshots.")
                positiveButton(text = "OK")
            }
            true
        }

        val category: PreferenceCategory = findPreference("extensions")
        IntegrityCore.dataTypeRepository.getAllDataTypes()
                .filter { it.packageName != context!!.packageName } // except the built in types
                .forEach { addExtensionPreference(category, it.title, it.packageName) }
    }

    private fun addExtensionPreference(category: PreferenceCategory, title: String,
                                       packageName: String) {
        val prefExtension = Preference(context)
        prefExtension.title = "$title type"
        prefExtension.summary = "View app info"

        prefExtension.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ViewExternalUtil.viewAppInfo(context!!, packageName)
            true
        }

        category.addPreference(prefExtension)
    }
}