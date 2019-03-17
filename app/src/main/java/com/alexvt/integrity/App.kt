/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity

import android.app.Activity
import android.app.Application
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import androidx.fragment.app.Fragment
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.util.ThemeUtil
import com.alexvt.integrity.lib.log.Log
import com.alexvt.integrity.lib.util.ThemeColors
import dagger.android.*
import dagger.android.support.HasSupportFragmentInjector
import java.lang.RuntimeException
import javax.inject.Inject


class App : Application(), HasActivityInjector, HasSupportFragmentInjector, HasBroadcastReceiverInjector {

    @Inject
    lateinit var activityInjector : DispatchingAndroidInjector<Activity>

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    @Inject
    lateinit var broadcastReceiverInjector: DispatchingAndroidInjector<BroadcastReceiver>

    override fun broadcastReceiverInjector(): AndroidInjector<BroadcastReceiver> = broadcastReceiverInjector

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector


    @Inject
    lateinit var integrityCore: IntegrityCore
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()

        if (isRestartProcess(this)) {
            if (checkRestartingInLoop(this)) {
                throw RuntimeException("Too frequent attempts to restart Integrity app")
            }
            return // recovery process is only used to restart the main one, doesn't init anything
        }

        DaggerAppDependenciesComponent.builder()
                .appContext(this)
                .create(this)
                .inject(this)

        try {
            integrityCore.init()
            ThemeUtil.initThemeSupport(this)
            ThemeUtil.applyTheme(with(settingsRepository.get()) {
                ThemeColors(colorBackground, colorPrimary, colorAccent)
            })
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
            Log(context, throwable.message
                    ?: "Uncaught exception (null message)")
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