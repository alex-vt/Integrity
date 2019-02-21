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
import com.alexvt.integrity.lib.Log
import com.jaredrummler.cyanea.Cyanea
import java.lang.RuntimeException


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (isRestartProcess(this)) {
            if (checkRestartingInLoop(this)) {
                throw RuntimeException("Too frequent attempts to restart Integrity app")
            }
            return // recovery process is only used to restart the main one, doesn't init anything
        }

        Cyanea.init(this, resources)
        try {
            IntegrityCore.init(this)
        } catch (throwable: Throwable) {
            Log(this, "Failed to start Integrity app").logError(throwable)
            throw throwable
        }

        handleUncaughtExceptions(this)
    }

    private fun checkRestartingInLoop(context: Context): Boolean {
        val minTimeBetweenRestartAttempts = 10000
        val name = "last_restart_time"
        val key = "last_restart_time"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

        val lastTimeRestarted = prefs?.getLong(key, 0) ?: 0
        val currentTime = System.currentTimeMillis()
        prefs.edit().putLong(key, currentTime).apply()

        return (lastTimeRestarted + minTimeBetweenRestartAttempts > currentTime)
    }

    private fun handleUncaughtExceptions(context: Context) {
        Thread.setDefaultUncaughtExceptionHandler {
            thread, throwable ->
            Log(context, throwable.message ?: "Uncaught exception (null message)")
                    .thread(thread).logCrash(throwable)
            Runtime.getRuntime().exit(1)
        }
    }

    private fun isRestartProcess(context: Context): Boolean {
        val currentPid = android.os.Process.myPid()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = manager.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (processInfo.pid == currentPid &&
                        (processInfo.processName.endsWith(":recovery")
                                || processInfo.processName.endsWith(":restart"))) {
                    return true
                }
            }
        }
        return false
    }
}