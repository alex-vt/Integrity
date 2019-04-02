/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.orchestration

import com.alexvt.integrity.lib.core.operations.log.Log
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.operations.jobs.ScheduledJobManager
import com.alexvt.integrity.core.data.log.LogRepository
import com.alexvt.integrity.lib.core.data.metadata.SnapshotStatus
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.operations.notifications.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.operations.notifications.ErrorNotifier
import com.alexvt.integrity.lib.core.data.log.LogEntry
import com.alexvt.integrity.lib.core.operations.log.LogManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
@Singleton
class IntegrityCoreOrchestrationManager @Inject constructor(
        private val metadataRepository: MetadataRepository,
        private val logRepository: LogRepository,
        private val settingsRepository: SettingsRepository,
        private val scheduledJobManager: ScheduledJobManager,
        private val errorNotifier: ErrorNotifier,
        private val disabledScheduledJobsNotifier: DisabledScheduledJobsNotifier,
        private val logManager: LogManager
) {
    /**
     * Should be called before using any other functions.
     */
    fun init() {
        settingsRepository.addChangesListener(this.toString()) {
            notifyAboutDisabledScheduledJobs()
            scheduledJobManager.updateSchedule()
        }

        watchAndNotifyAboutUnreadErrors()

        resetInProgressSnapshotStatuses()

        Log(logManager, "IntegrityCore initialized").log()
    }

    private fun resetInProgressSnapshotStatuses() {
        metadataRepository.getAllArtifactMetadataBlocking()
                .filter { it.status == SnapshotStatus.IN_PROGRESS }
                .forEach {
                    metadataRepository.removeSnapshotMetadata(it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(
                            it.copy(status = SnapshotStatus.INCOMPLETE))
                }
    }

    private fun watchAndNotifyAboutUnreadErrors() {
        val logErrorLimitToNotify = 1000
        logRepository.getUnreadErrorsFlowable(logErrorLimitToNotify)
                .subscribe { unreadErrorLogEntries ->
                    notifyAboutUnreadErrors(unreadErrorLogEntries)
                }
    }

    private fun notifyAboutUnreadErrors(unreadErrorLogEntries: List<LogEntry>) {
        if (unreadErrorLogEntries.isNotEmpty()) {
            if (settingsRepository.get().notificationShowErrors) {
                errorNotifier.notifyAboutErrors(unreadErrorLogEntries)
            } else {
                errorNotifier.removeNotification()
            }
        } else {
            errorNotifier.removeNotification()
        }
    }

    private fun notifyAboutDisabledScheduledJobs() {
        val showDisabledScheduledJobsNotification = !settingsRepository.get().jobsEnableScheduled
                && settingsRepository.get().notificationShowDisabledScheduled
        if (showDisabledScheduledJobsNotification) {
            disabledScheduledJobsNotifier.showNotification()
        } else {
            disabledScheduledJobsNotifier.removeNotification()
        }
    }

}