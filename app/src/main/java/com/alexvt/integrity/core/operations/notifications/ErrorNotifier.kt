/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.notifications

import com.alexvt.integrity.lib.core.data.log.LogEntry

interface ErrorNotifier {

    fun notifyAboutErrors(errorLogEntries: List<LogEntry>)

    fun getNotificationText(errorLogEntries: List<LogEntry>): String {
        val firstErrorText = errorLogEntries[0].text
        val timeStampSuffix = if (errorLogEntries[0].time.isNotBlank()) {
            "\nat ${errorLogEntries[0].time}"
        } else {
            ""
        }
        val moreErrorsSuffix = if (errorLogEntries.size > 1) {
            "\n(${errorLogEntries.size - 1} more)"
        } else {
            ""
        }
        return firstErrorText + timeStampSuffix + moreErrorsSuffix
    }

    fun removeNotification()

}