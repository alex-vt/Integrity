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

class AppearanceSettingsFragment : SettingsFragment() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    override val vm by lazy {
        ViewModelProviders.of(activity!!, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        setPreferencesFromResource(R.xml.settings_appearance, rootKey)

        bindColorBackground()
        bindColorPrimary()
        bindColorAccent()
        bindTextFont()
    }

    private fun bindColorBackground() = bindColorSetting(
            key = "appearance_color_background",
            initialColor = vm.computeColorBackground(),
            colorPalette = vm.colorsBackground,
            selectionAction = { vm.saveColorBackground(it) }
    )

    private fun bindColorPrimary() = bindColorSetting(
            key = "appearance_color_main",
            initialColor = vm.computeColorPrimary(),
            colorPalette = vm.colorsPalette,
            selectionAction = { vm.saveColorPrimary(it) }
    )

    private fun bindColorAccent() = bindColorSetting(
            key = "appearance_color_accent",
            initialColor = vm.computeColorAccent(),
            colorPalette = vm.colorsPalette,
            selectionAction = { vm.saveColorAccent(it) }
    )

    private fun bindTextFont() = bindSelectionSetting(
            key = "appearance_text_font",
            title = "Select font",
            items = vm.getAllFontNames(),
            initialSelection = vm.getCurrentFontIndex(),
            settingSelector = { it.textFont },
            selectionAction = { vm.saveFont(it) }
    )
}