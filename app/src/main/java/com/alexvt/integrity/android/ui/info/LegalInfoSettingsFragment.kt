/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.info

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alexvt.integrity.R
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class LegalInfoSettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[LegalInfoViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_info_legal, rootKey)

        val prefTerms: Preference = findPreference("legal_terms")
        val prefPrivacy: Preference = findPreference("legal_privacy")

        prefTerms.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            vm.viewTerms()
            true
        }
        prefPrivacy.onPreferenceClickListener = Preference.OnPreferenceClickListener {
           vm.viewPrivacyPolicy()
            true
        }
    }
}