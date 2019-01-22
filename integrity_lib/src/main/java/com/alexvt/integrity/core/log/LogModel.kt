/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.log

import java.io.Serializable

/**
 * Log entry with logged data attached as a text key value list.
 */
data class LogEntry(val time: String = "",
                    val data: LinkedHashMap<String, String> = linkedMapOf(),
                    val type: String = LogEntryType.NORMAL
) : Serializable

/**
 * Standard names for log data values
 */
object LogKey {
    const val ARTIFACT_ID = "artifact_id"
    const val SNAPSHOT_DATE = "snapshot_date"
    const val CLASS = "class"
    const val PACKAGE = "package"
    const val METHOD = "method"
    const val THREAD = "thread"
    const val DESCRIPTION = "description"
    const val DATA_TYPE = "data_type"
    const val STACK_TRACE = "stack_trace"
}

object LogEntryType {
    const val DEBUG = "debug" // for app development purposes
    const val NORMAL = "normal" // regular planned event or action
    const val ERROR = "error" // abnormal state or behavior
    const val CRASH = "crash" // app crash
}
