/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alexvt.integrity.core.log.LogEntry
import com.alexvt.integrity.lib.R

object DisabledScheduledJobsNotifier {

    private const val CHANNEL_ID = "disabled_scheduled_jobs"
    private const val NOTIFICATION_ID = 1

    fun showNotification(context: Context) {
        val enableScheduledJobsPendingIntent = PendingIntent.getBroadcast(context, 0, Intent().apply {
            action = "com.alexvt.integrity.ENABLE_SCHEDULED_JOBS"
        }, 0)

        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_error)
                .setContentTitle("Limitations - Integrity")
                .setContentText("Scheduled jobs are disabled")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .addAction(0, "Enable again", enableScheduledJobsPendingIntent)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, mBuilder.build())
        }
    }

    fun removeNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }
    }

}