/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity

import android.app.Application
import com.alexvt.integrity.core.*
import android.app.ActivityManager
import android.content.Context



class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (isRecoveryProcess(this)) {
            return // recovery process is only used to restart the main one, doesn't init anything
        }

        IntegrityCore.init(this)
    }

    private fun isRecoveryProcess(context: Context): Boolean {
        val currentPid = android.os.Process.myPid()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = manager.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (processInfo.pid == currentPid && processInfo.processName.endsWith(":recovery")) {
                    return true
                }
            }
        }
        return false
    }
}