/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.lib.Log

/**
 * Enables app running after it was interrupted by system events.
 */
object AutoStartUtil {

    class AutoStartReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log(context).what("Auto start")
                    .where(this, "onReceive").logDebug()
            // IntegrityCore initialized in App onStart
        }
    }

}