/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.recovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.ui.main.MainActivity
import com.alexvt.integrity.lib.log.Log

object AppRestartUtil {

    fun restartApp(context: Context) {
        context.applicationContext.sendBroadcast(Intent().apply {
            action = "com.alexvt.integrity.APP_REOPEN"
        })
        Runtime.getRuntime().exit(0) // the current process not needed anymore
    }

}

class AppReopenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.v(AppReopenReceiver::class.java.simpleName,
                "Sending broadcast for AppOpenReceiver which will open the app main activity...")
        context.applicationContext.sendBroadcast(Intent().apply {
            action = "com.alexvt.integrity.APP_OPEN"
        })

        Runtime.getRuntime().exit(0) // restart process not needed anymore
    }
}

class AppOpenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log(context, "App was (re)started explicitly").log()
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}