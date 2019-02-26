/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.settings

import android.content.ComponentName
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.alexvt.integrity.R
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.alexvt.integrity.core.IntegrityCore
import android.content.Intent
import android.net.Uri
import com.afollestad.materialdialogs.MaterialDialog


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
        IntegrityCore.getTypeNames()
                .filter { it.packageName != context!!.packageName } // except the built in types
                .forEach { addExtensionPreference(category, it) }
    }

    private fun addExtensionPreference(category: PreferenceCategory, typeName: ComponentName) {
        val prefExtension = Preference(context)
        val name = typeName.className.substringAfterLast(".").removeSuffix("TypeActivity")
        prefExtension.title = "$name type"
        prefExtension.summary = "View app info"

        prefExtension.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            openAppInfo(typeName.packageName)
            true
        }

        category.addPreference(prefExtension)
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
}