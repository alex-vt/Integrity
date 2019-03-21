/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.content.Context
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.jobs.ScheduledJobManager
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.lib.log.*
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.core.notifications.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.notifications.ErrorNotifier
import com.alexvt.integrity.core.settings.SettingsRepository
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
@Singleton
class IntegrityCore @Inject constructor(
        private val context: Context,
        private val metadataRepository: MetadataRepository,
        private val logRepository: LogRepository,
        private val settingsRepository: SettingsRepository,
        private val scheduledJobManager: ScheduledJobManager
) {
    private lateinit var logUnreadErrorSubscription: Disposable

    /**
     * Should be called before using any other functions.
     */
    fun init() {
        resetInProgressSnapshotStatuses() // if there are any in progress snapshots, they are rogue

        settingsRepository.addChangesListener(this.toString()) {
            notifyAboutDisabledScheduledJobs(context)
            scheduledJobManager.updateSchedule(context)
        }

        val logErrorLimitToNotify = 1000
        logUnreadErrorSubscription = logRepository.getUnreadErrors(logErrorLimitToNotify).subscribe {
            unreadErrors -> notifyAboutUnreadErrors(unreadErrors, context)
        }

        Log(context, "IntegrityCore initialized").log()
    }

    fun clear() {
        logUnreadErrorSubscription.dispose()
    }

    private fun resetInProgressSnapshotStatuses() {
        metadataRepository.getAllArtifactMetadata()
                .filter { it.status == SnapshotStatus.IN_PROGRESS }
                .forEach {
                    metadataRepository.removeSnapshotMetadata(it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(
                            it.copy(status = SnapshotStatus.INCOMPLETE))
                }
    }

    private fun notifyAboutUnreadErrors(unreadErrors: List<LogEntry>, context: Context) {
        if (unreadErrors.isNotEmpty()) {
            if (settingsRepository.get().notificationShowErrors) {
                ErrorNotifier.notifyAboutErrors(context, unreadErrors)
            } else {
                ErrorNotifier.removeNotification(context)
            }
        } else {
            ErrorNotifier.removeNotification(context)
        }
    }

    private fun notifyAboutDisabledScheduledJobs(context: Context) {
        val showDisabledScheduledJobsNotification = !settingsRepository.get().jobsEnableScheduled
                && settingsRepository.get().notificationShowDisabledScheduled
        if (showDisabledScheduledJobsNotification) {
            DisabledScheduledJobsNotifier.showNotification(context)
        } else {
            DisabledScheduledJobsNotifier.removeNotification(context)
        }
    }

}