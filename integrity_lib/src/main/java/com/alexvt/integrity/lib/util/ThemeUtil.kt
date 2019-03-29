/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.util

import android.app.Application
import android.graphics.Color
import android.view.View
import androidx.core.graphics.ColorUtils
import com.jaredrummler.cyanea.Cyanea
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity
import com.leinardi.android.speeddial.SpeedDialActionItem

data class ThemeColors(
        val colorBackground: String,
        val colorPrimary: String,
        val colorAccent: String
)

object ThemeUtil { // todo use parsed colors

    fun getIntColor(stringColor: String) = Color.parseColor(stringColor)

    fun getColorBackground(colors: ThemeColors) = Color.parseColor(colors.colorBackground)

    fun getColorBackgroundSecondary(colors: ThemeColors): Int {
        val colorBackground = getColorBackground(colors)
        return if (ColorUtils.calculateLuminance(colorBackground) < 0.5) {
            lighten(colorBackground, 0.2f)
        } else {
            darken(colorBackground, 0.06f)
        }
    }

    fun getColorBackgroundBleached(colors: ThemeColors): Int {
        val colorBackground = getColorBackground(colors)
        return if (ColorUtils.calculateLuminance(colorBackground) < 0.5) {
            colorBackground
        } else {
            Color.WHITE
        }
    }

    fun getColorPrimary(colors: ThemeColors) = Color.parseColor(colors.colorPrimary)

    fun getColorPrimaryDark(colors: ThemeColors) = darken(getColorPrimary(colors), 0.2f)

    fun getColorAccent(colors: ThemeColors) = Color.parseColor(colors.colorAccent)

    fun getTextColorPrimary(colors: ThemeColors) = softInvertGray(getColorBackground(colors))

    fun getTextColorSecondary(colors: ThemeColors): Int {
        val textColorPrimary = getTextColorPrimary(colors)
        return if (ColorUtils.calculateLuminance(textColorPrimary) < 0.5) {
            lighten(textColorPrimary, 0.3f)
        } else {
            darken(textColorPrimary, 0.2f)
        }
    }

    fun applyToSpeedDial(actionBuilder: SpeedDialActionItem.Builder, colors: ThemeColors)
            = actionBuilder
            .setFabBackgroundColor(getColorAccent(colors))
            .setFabImageTintColor(Color.WHITE)
            .setLabelBackgroundColor(getTextColorSecondary(colors))
            .setLabelColor(getColorBackground(colors))!!

    fun applyToView(view: View) = Cyanea.instance.tinter.tint(view)

    fun applyThemeAndRecreate(activity: CyaneaAppCompatActivity, colors: ThemeColors) {
        applyTheme(colors).recreate(activity)
    }

    fun initThemeSupport(application: Application) {
        Cyanea.init(application, application.resources)
    }

    fun isThemeApplied(colors: ThemeColors) = with(Cyanea.instance) {
        primary == getColorPrimary(colors)
                && primaryDark == getColorPrimaryDark(colors)
                && accent == getColorAccent(colors)
                && shouldTintNavBar
                && shouldTintStatusBar
                && navigationBar == getColorPrimaryDark(colors)
                && backgroundColor == getColorBackground(colors)
    }

    fun applyTheme(colors: ThemeColors): Cyanea.Recreator = Cyanea.instance.edit {
        primary(getColorPrimary(colors))
        primaryDark(getColorPrimaryDark(colors))
        accent(getColorAccent(colors))
        shouldTintNavBar(true)
        shouldTintStatusBar(true)
        navigationBar(getColorPrimaryDark(colors))
        background(getColorBackground(colors))
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

    fun getHexColor(intColor: Int) = String.format("#%06X", (0xFFFFFF and intColor))
}