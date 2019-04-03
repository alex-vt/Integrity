/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.util

import android.app.Application
import android.graphics.Color
import android.view.View
import com.alexvt.integrity.lib.core.util.ColorUtil
import com.alexvt.integrity.lib.core.util.ThemeColors
import com.jaredrummler.cyanea.Cyanea
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity
import com.leinardi.android.speeddial.SpeedDialActionItem

object ThemeUtil { // todo use parsed colors

    fun getIntColor(stringColor: String) = Color.parseColor(stringColor)

    fun applyToSpeedDial(actionBuilder: SpeedDialActionItem.Builder, colors: ThemeColors)
            = actionBuilder
            .setFabBackgroundColor(ColorUtil.getColorAccent(colors))
            .setFabImageTintColor(Color.WHITE)
            .setLabelBackgroundColor(ColorUtil.getTextColorSecondary(colors))
            .setLabelColor(ColorUtil.getColorBackground(colors))!!

    fun applyToView(view: View) = Cyanea.instance.tinter.tint(view)

    fun applyThemeAndRecreate(activity: CyaneaAppCompatActivity, colors: ThemeColors) {
        applyTheme(colors).recreate(activity)
    }

    fun initThemeSupport(application: Application) {
        Cyanea.init(application, application.resources)
    }

    fun isThemeApplied(colors: ThemeColors) = with(Cyanea.instance) {
        primary == ColorUtil.getColorPrimary(colors)
                && primaryDark == ColorUtil.getColorPrimaryDark(colors)
                && accent == ColorUtil.getColorAccent(colors)
                && shouldTintNavBar
                && shouldTintStatusBar
                && navigationBar == ColorUtil.getColorPrimaryDark(colors)
                && backgroundColor == ColorUtil.getColorBackground(colors)
    }

    fun applyTheme(colors: ThemeColors): Cyanea.Recreator = Cyanea.instance.edit {
        primary(ColorUtil.getColorPrimary(colors))
        primaryDark(ColorUtil.getColorPrimaryDark(colors))
        accent(ColorUtil.getColorAccent(colors))
        shouldTintNavBar(true)
        shouldTintStatusBar(true)
        navigationBar(ColorUtil.getColorPrimaryDark(colors))
        background(ColorUtil.getColorBackground(colors))
    }
}