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

object ErrorNotifier {

    private const val CHANNEL_ID = "error"
    private const val NOTIFICATION_ID = 0

    fun notifyAboutErrors(context: Context, errorLogEntries: List<LogEntry>) {
        val intent = Intent().apply {
            component = ComponentName("com.alexvt.integrity",
                    "com.alexvt.integrity.base.activity.LogViewActivity") // todo resolve
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val logViewPendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_error)
                .setContentTitle("Error happened - Integrity")
                .setContentText(getNotificationText(errorLogEntries))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(getNotificationText(errorLogEntries)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(logViewPendingIntent)
                .addAction(0, "View log", logViewPendingIntent)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, mBuilder.build())
        }
    }

    private fun getNotificationText(errorLogEntries: List<LogEntry>): String {
        val firstErrorText = errorLogEntries[0].data.toList()[0].second.take(300) // todo improve
        val moreErrorsSuffix = if (errorLogEntries.size > 1) {
            "\n(${errorLogEntries.size - 1} more)"
        } else {
            ""
        }
        return firstErrorText + moreErrorsSuffix
    }

    fun removeNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }
    }

}