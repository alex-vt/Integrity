/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.app.Activity
import android.content.Context
import android.widget.TextView
import android.view.ViewGroup
import android.graphics.Typeface
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.R


object FontUtil {

    private val fontNameMap = linkedMapOf(
            Pair("Classic", R.font.roboto_regular),
            Pair("Modern", R.font.manrope_medium)
    )

    fun getNames() = fontNameMap.map { it.key }

    fun saveFont(context: Context, fontName: String) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                textFont = fontName)
        )
    }

    // todo keep typeface until it changes in settings
    fun getTypeface(context: Context): Typeface {
        val fontRes = getFontResOrNull()
        return if (fontRes != null) {
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        } else {
            Typeface.DEFAULT
        }
    }

    fun setFont(activity: Activity) {
        setFont(activity, activity.findViewById(android.R.id.content))
    }

    fun setFont(context: Context, view: View) {
        setFontRecursiveInView(view, getTypeface(context))
    }

    private fun getFontResOrNull(): Int? {
        val fontName = IntegrityCore.settingsRepository.get().textFont
        return if (fontNameMap.containsKey(fontName)) fontNameMap[fontName] else null
    }

    private fun setFontRecursiveInViewGroup(viewTree: ViewGroup, typeface: Typeface) {
        for (i in 0 until viewTree.childCount) {
            setFontRecursiveInView(viewTree.getChildAt(i), typeface)
        }
    }

    private fun setFontRecursiveInView(view: View, typeface: Typeface) {
        if (view is ViewGroup) {
            // recursive call
            setFontRecursiveInViewGroup(view, typeface)
        } else if (view is TextView) {
            // base case
            view.typeface = typeface
        }
    }
}