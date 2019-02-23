/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.info

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R


class LegalInfoSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_info_legal, rootKey)

        val prefTerms: Preference = findPreference("legal_terms")
        val prefPrivacy: Preference = findPreference("legal_privacy")

        prefTerms.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialDialog(context!!)
                    .title(text = "Terms & Conditions")
                    .message(text = getTextFromRawResource(context!!, R.raw.license))
                    .positiveButton(text = "OK")
                    .show()
            true
        }
        prefPrivacy.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialDialog(context!!)
                    .title(text = "Privacy Policy")
                    .message(text = getTextFromRawResource(context!!, R.raw.privacy)) // todo put online
                    .positiveButton(text = "OK")
                    .show()
            true
        }
    }

    private fun getTextFromRawResource(context: Context, resId: Int): String {
        val inputStream = context.resources.openRawResource(resId)
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        return String(bytes)
    }
}