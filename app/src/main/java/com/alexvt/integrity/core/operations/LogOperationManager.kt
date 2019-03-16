/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations

import android.content.Context
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.core.notifications.ErrorNotifier
import javax.inject.Inject

class LogOperationManager @Inject constructor(
        private val logRepository: LogRepository,
        private val context: Context
) {

    fun markErrorsRead() {
        android.util.Log.v("IntegrityCore", "Errors marked read")
        logRepository.markAllRead()
        ErrorNotifier.removeNotification(context)
    }

}