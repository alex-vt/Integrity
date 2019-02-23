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
import com.alexvt.integrity.lib.R


object FontUtil {

    private val fontNameMap = linkedMapOf(
            Pair("Classic", R.font.roboto_regular),
            Pair("Modern", R.font.manrope_medium)
    )

    fun getNames() = fontNameMap.map { it.key }

    // todo keep typeface until it changes in settings
    fun getTypeface(context: Context, fontName: String): Typeface {
        val fontRes = getFontResOrNull(fontName)
        return if (fontRes != null) {
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        } else {
            Typeface.DEFAULT
        }
    }

    fun setFont(activity: Activity, fontName: String) {
        setFont(activity, activity.findViewById(android.R.id.content), fontName)
    }

    fun setFont(context: Context, view: View, fontName: String) {
        setFontRecursiveInView(view, getTypeface(context, fontName))
    }

    private fun getFontResOrNull(fontName: String): Int? {
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