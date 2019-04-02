/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.lib.core.operations.log.Log
import com.alexvt.integrity.lib.core.operations.log.LogManager
import dagger.android.AndroidInjection
import javax.inject.Inject

class AutoStartReceiver : BroadcastReceiver() {
    @Inject
    lateinit var logManager: LogManager

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        Log(logManager, "Auto start").log()
        // IntegrityCore initialized in App onStart
    }
}