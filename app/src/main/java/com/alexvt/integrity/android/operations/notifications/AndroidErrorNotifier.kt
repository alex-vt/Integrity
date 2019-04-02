/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.operations.notifications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alexvt.integrity.core.operations.notifications.ErrorNotifier
import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.R
import javax.inject.Inject

class AndroidErrorNotifier @Inject constructor(
        private val context: Context
) : ErrorNotifier {

    private val CHANNEL_ID = "error"
    private val NOTIFICATION_ID = 0

    override fun notifyAboutErrors(errorLogEntries: List<LogEntry>) {
        val logViewPendingIntent = PendingIntent.getActivity(context, 0, Intent().apply {
            component = ComponentName("com.alexvt.integrity",
                    "com.alexvt.integrity.android.ui.log.LogViewActivity") // todo resolve
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }, 0)
        val logReadPendingIntent = PendingIntent.getBroadcast(context, 0, Intent().apply {
            action = "com.alexvt.integrity.LOG_READ"
        }, 0)

        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_error)
                .setContentTitle("Error happened - Integrity")
                .setContentText(getNotificationText(errorLogEntries))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(getNotificationText(errorLogEntries)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(logViewPendingIntent)
                .addAction(0, "OK", logReadPendingIntent)
                .addAction(0, "View log", logViewPendingIntent)
                .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, mBuilder.build())
        }
    }

    override fun removeNotification() {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }
    }

}