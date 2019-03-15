/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui

import androidx.lifecycle.ViewModel
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.util.ThemeColors
import com.alexvt.integrity.lib.util.ThemeUtil

abstract class ThemedViewModel : ViewModel() {
    abstract val settingsRepository: SettingsRepository

    fun getFont() = settingsRepository.get().textFont

    fun computeColorPrimaryDark() = ThemeUtil.getColorPrimaryDark(getThemeColors())

    fun computeTextColorPrimary() = ThemeUtil.getTextColorPrimary(getThemeColors())

    fun computeTextColorSecondary() = ThemeUtil.getTextColorSecondary(getThemeColors())

    fun computeColorAccent() = ThemeUtil.getColorAccent(getThemeColors())

    fun computeColorBackground() = ThemeUtil.getColorBackground(getThemeColors())

    fun computeColorBackgroundSecondary() = ThemeUtil.getColorBackgroundSecondary(getThemeColors())

    fun computeColorBackgroundBleached() = ThemeUtil.getColorBackgroundBleached(getThemeColors())

    fun getThemeColors() = with(settingsRepository.get()) {
        ThemeColors(colorBackground, colorPrimary, colorAccent)
    }
}
