/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import android.app.Application
import com.alexvt.integrity.lib.android.util.ThemeUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeUtil.initThemeSupport(this)
    }
}