/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.data.log

import java.io.Serializable

/**
 * Log entry with logged data attached as a text key value list.
 *
 * Order ID is ascending by addition order.
 */
data class LogEntry(val orderId: String = "",
                    val time: String = "",
                    val tag: String = "",
                    val text: String = "",
                    val data: LinkedHashMap<String, String> = linkedMapOf(),
                    val stackTraceText: String = "",
                    val type: String = LogEntryType.NORMAL,
                    val read: Boolean = false
) : Serializable

/**
 * Standard names for build data values
 */
object LogKey {
    const val ARTIFACT_ID = "artifact_id"
    const val SNAPSHOT_DATE = "snapshot_date"
    const val PACKAGE = "package"
    const val THREAD = "thread"
    const val PROCESS = "process"
}

object LogEntryType {
    const val VERBOSE = "verbose" // for app development purposes
    const val NORMAL = "normal" // regular planned event or action
    const val ERROR = "error" // abnormal state or behavior
    const val CRASH = "crash" // app crash
}
