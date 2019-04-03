/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.util

data class ThemeColors(
        val colorBackground: String,
        val colorPrimary: String,
        val colorAccent: String
)

object ColorUtil {

    private const val COLOR_BLACK = -0x1000000
    private const val COLOR_WHITE = -0x1

    fun getColorBackground(colors: ThemeColors) = parseColor(colors.colorBackground)

    fun getColorBackgroundSecondary(colors: ThemeColors): Int {
        val colorBackground = getColorBackground(colors)
        return if (calculateLuminance(colorBackground) < 0.5) {
            lighten(colorBackground, 0.2f)
        } else {
            darken(colorBackground, 0.06f)
        }
    }

    fun getColorBackgroundBleached(colors: ThemeColors): Int {
        val colorBackground = getColorBackground(colors)
        return if (calculateLuminance(colorBackground) < 0.5) {
            colorBackground
        } else {
            COLOR_WHITE
        }
    }

    fun getColorPrimary(colors: ThemeColors) = parseColor(colors.colorPrimary)

    fun getColorPrimaryDark(colors: ThemeColors) = darken(getColorPrimary(colors), 0.2f)

    fun getColorAccent(colors: ThemeColors) = parseColor(colors.colorAccent)

    fun getTextColorPrimary(colors: ThemeColors) = softInvertGray(getColorBackground(colors))

    fun getTextColorSecondary(colors: ThemeColors): Int {
        val textColorPrimary = getTextColorPrimary(colors)
        return if (calculateLuminance(textColorPrimary) < 0.5) {
            lighten(textColorPrimary, 0.3f)
        } else {
            darken(textColorPrimary, 0.2f)
        }
    }

    // Android-independent copy of android.graphics.Color.parseColor
    private fun parseColor(colorString: String): Int {
        // Use a long to avoid rollovers on #ffXXXXXX
        var color = java.lang.Long.parseLong(colorString.substring(1), 16)
        if (colorString.length == 7) {
            // Set the alpha value
            color = color or -0x1000000
        } else if (colorString.length != 9) {
            throw IllegalArgumentException("Unknown color")
        }
        return color.toInt()
    }

    private fun color(a: Int, r: Int, g: Int, b: Int) = a shl 24 or (r shl 16) or (g shl 8) or b

    // see https://stackoverflow.com/questions/596216
    private fun calculateLuminance(color: Int)
            = (0.2126 * r(color) + 0.7152 * g(color) + 0.0722 * b(color)) / 255

    private fun a(color: Int) = color shr 24 and 0xFF
    private fun r(color: Int) = color shr 16 and 0xFF
    private fun g(color: Int) = color shr 8 and 0xFF
    private fun b(color: Int) = color shr 0 and 0xFF

    private fun blend(color1: Int, color2: Int, ratio: Float) = color(
            (a(color1) * (1 - ratio) + a(color2) * ratio).toInt(),
            (r(color1) * (1 - ratio) + r(color2) * ratio).toInt(),
            (g(color1) * (1 - ratio) + g(color2) * ratio).toInt(),
            (b(color1) * (1 - ratio) + b(color2) * ratio).toInt()
    )

    private fun darken(colorInt: Int, strength: Float) = blend(colorInt, COLOR_BLACK, strength)

    private fun lighten(colorInt: Int, strength: Float) = blend(colorInt, COLOR_WHITE, strength)

    private fun invert(colorInt: Int) = colorInt xor 0x00ffffff

    /**
     * Inverts color as gray scale and keeps luminance difference to the original no more than 0.9.
     */
    private fun softInvertGray(colorInt: Int): Int {
        val originalLuminance = calculateLuminance(colorInt)

        val inversionLuminanceDifference = Math.abs(1 - 2 * originalLuminance)
        val limitLuminanceDifference = if (originalLuminance < 0.5) 0.95 else 0.85 // dark to light can be more white
        val targetLuminanceDifference = Math.min(inversionLuminanceDifference, limitLuminanceDifference)

        val targetLuminance = if (originalLuminance < 0.5) {
            originalLuminance + targetLuminanceDifference // dark to light
        } else {
            originalLuminance - targetLuminanceDifference // light to dark
        }
        val targetLuminanceInt = (targetLuminance * 255).toInt()
        return color(255, targetLuminanceInt, targetLuminanceInt, targetLuminanceInt)
    }

    fun getHexColor(intColor: Int) = String.format("#%06X", (0xFFFFFF and intColor))
}