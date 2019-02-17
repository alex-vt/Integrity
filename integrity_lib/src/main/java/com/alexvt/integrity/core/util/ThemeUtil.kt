/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.alexvt.integrity.core.IntegrityCore

object ThemeUtil { // todo keep parsed colors until color settings change

    fun getColorBackground() = Color.parseColor(IntegrityCore.settingsRepository.get().colorBackground)

    fun getColorBackgroundSecondary(): Int {
        val colorBackground = getColorBackground()
        return if (ColorUtils.calculateLuminance(colorBackground) < 0.5) {
            lighten(colorBackground, 0.2f)
        } else {
            darken(colorBackground, 0.06f)
        }
    }

    fun getColorPrimary() = Color.parseColor(IntegrityCore.settingsRepository.get().colorPrimary)

    fun getColorPrimaryDark() = darken(getColorPrimary(), 0.2f)

    fun getColorAccent() = Color.parseColor(IntegrityCore.settingsRepository.get().colorAccent)

    fun getTextColorPrimary() = softInvertGray(getColorBackground())

    fun getTextColorSecondary(): Int {
        val textColorPrimary = getTextColorPrimary()
        return if (ColorUtils.calculateLuminance(textColorPrimary) < 0.5) {
            lighten(textColorPrimary, 0.3f)
        } else {
            darken(textColorPrimary, 0.2f)
        }
    }

    fun saveColorBackground(context: Context, intColor: Int) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                colorBackground = getHexColor(intColor))
        )
    }

    fun saveColorPrimary(context: Context, intColor: Int) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                colorPrimary = getHexColor(intColor))
        )
    }

    fun saveColorAccent(context: Context, intColor: Int) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                colorAccent = getHexColor(intColor))
        )
    }

    private fun darken(colorInt: Int, strength: Float)
            = ColorUtils.blendARGB(colorInt, Color.BLACK, strength)

    private fun lighten(colorInt: Int, strength: Float)
            = ColorUtils.blendARGB(colorInt, Color.WHITE, strength)

    private fun invert(colorInt: Int) = colorInt xor 0x00ffffff

    /**
     * Inverts color as gray scale and keeps luminance difference to the original no more than 0.9.
     */
    private fun softInvertGray(colorInt: Int): Int {
        val originalLuminance = ColorUtils.calculateLuminance(colorInt)

        val inversionLuminanceDifference = Math.abs(1 - 2 * originalLuminance)
        val limitLuminanceDifference = if (originalLuminance < 0.5) 0.95 else 0.85 // dark to light can be more white
        val targetLuminanceDifference = Math.min(inversionLuminanceDifference, limitLuminanceDifference)

        val targetLuminance = if (originalLuminance < 0.5) {
            originalLuminance + targetLuminanceDifference // dark to light
        } else {
            originalLuminance - targetLuminanceDifference // light to dark
        }
        val targetLuminanceInt = (targetLuminance * 255).toInt()
        return Color.argb(255, targetLuminanceInt, targetLuminanceInt, targetLuminanceInt)
    }

    private fun getHexColor(intColor: Int) = String.format("#%06X", (0xFFFFFF and intColor))
}