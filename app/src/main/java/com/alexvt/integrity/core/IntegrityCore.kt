/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.content.Context
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.core.jobs.ScheduledJobManager
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.lib.log.*
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.core.notifications.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.notifications.ErrorNotifier
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
@Singleton
class IntegrityCore @Inject constructor(
        private val context: Context,
        val metadataRepository: MetadataRepository,
        val credentialsRepository: CredentialsRepository,
        val searchIndexRepository: SearchIndexRepository,
        val logRepository: LogRepository,
        val settingsRepository: SettingsRepository,
        private val scheduledJobManager: ScheduledJobManager
) {

    /**
     * Should be called before using any other functions.
     */
    fun init() {
        logRepository.init()
        settingsRepository.init()
        metadataRepository.init()
        credentialsRepository.init()
        searchIndexRepository.init()

        resetInProgressSnapshotStatuses() // if there are any in progress snapshots, they are rogue

        settingsRepository.addChangesListener(this.toString()) {
            notifyAboutDisabledScheduledJobs(context)
            scheduledJobManager.updateSchedule(context)
        }

        logRepository.addChangesListener(this.toString()) {
            notifyAboutUnreadErrors(context)
        }

        Log(context, "IntegrityCore initialized").log()
    }

    private fun resetInProgressSnapshotStatuses() {
        metadataRepository.getAllArtifactMetadata().snapshots
                .filter { it.status == SnapshotStatus.IN_PROGRESS }
                .forEach {
                    metadataRepository.removeSnapshotMetadata(it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(
                            it.copy(status = SnapshotStatus.INCOMPLETE))
                }
    }

    private fun notifyAboutUnreadErrors(context: Context) {
        val unreadErrors = logRepository.getUnreadErrors()
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