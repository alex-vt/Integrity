/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.log

import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.core.operations.LogOperationManager
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.log.LogEntry
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import io.reactivex.Scheduler
import javax.inject.Inject

data class NavigationEvent(
        val goBack: Boolean = false
)

class LogViewModel @Inject constructor(
        private val uiScheduler: Scheduler,
        override val settingsRepository: SettingsRepository,
        private val logRepository: LogRepository,
        private val logOperationManager: LogOperationManager
        ) : ThemedViewModel() {
    private val logEntriesLimit = 1000

    val logData = MutableLiveData<List<LogEntry>>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()


    private val logSubscription = logRepository.getRecentEntries(logEntriesLimit)
            .observeOn(uiScheduler)
            .subscribe { logData.value = it }

    init {
        logOperationManager.markErrorsRead()
    }

    override fun onCleared() {
        logSubscription.dispose()
        super.onCleared()
    }



    // user actions

    fun clickDeleteAllLog() {
        logRepository.clear()
    }

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
