/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.log

import com.alexvt.integrity.core.data.log.LogRepository
import com.alexvt.integrity.core.operations.notifications.ErrorNotifier
import com.alexvt.integrity.lib.core.operations.log.Logger
import javax.inject.Inject

class LogReadMarker @Inject constructor(
        private val logRepository: LogRepository,
        private val errorNotifier: ErrorNotifier,
        private val logger: Logger
) {

    fun markErrorsRead() {
        logger.v("Errors marked read")
        logRepository.markAllRead()
        errorNotifier.removeNotification()
    }

}