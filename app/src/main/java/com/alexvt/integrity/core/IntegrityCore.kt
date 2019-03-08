/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.annotation.SuppressLint
import android.content.Context
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.metadata.SimplePersistableMetadataRepository
import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.core.credentials.SimplePersistableCredentialsRepository
import com.alexvt.integrity.core.jobs.ScheduledJobManager
import com.alexvt.integrity.lib.filesystem.AndroidFilesystemManager
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.core.jobs.AndroidScheduledJobManager
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.core.log.SimplePersistableLogRepository
import com.alexvt.integrity.lib.log.*
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.core.notifications.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.notifications.ErrorNotifier
import com.alexvt.integrity.core.operations.AndroidSnapshotOperationManager
import com.alexvt.integrity.core.operations.SnapshotOperationManager
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.search.SimplePersistableSearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.core.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.core.types.AndroidDataTypeRepository
import com.alexvt.integrity.core.types.DataTypeRepository
import com.alexvt.integrity.lib.util.*

/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
@SuppressLint("StaticFieldLeak")
object IntegrityCore {
    // todo replace early implementations
    val metadataRepository: MetadataRepository by lazy { SimplePersistableMetadataRepository(context) }
    val credentialsRepository: CredentialsRepository by lazy { SimplePersistableCredentialsRepository(context) }
    val searchIndexRepository: SearchIndexRepository by lazy { SimplePersistableSearchIndexRepository(context) }
    val logRepository: LogRepository by lazy { SimplePersistableLogRepository(context) }
    val settingsRepository: SettingsRepository by lazy { SimplePersistableSettingsRepository(context) }
    val dataTypeRepository: DataTypeRepository by lazy { AndroidDataTypeRepository(context) }

    val dataFolderManager: DataFolderManager by lazy { DataFolderManager(AndroidFilesystemManager(context)) }
    val scheduledJobManager: ScheduledJobManager by lazy { AndroidScheduledJobManager() }
    val snapshotOperationManager: SnapshotOperationManager by lazy {AndroidSnapshotOperationManager(context) }

    private lateinit var context: Context

    /**
     * Should be called before using any other functions.
     */
    fun init(context: Context) {
        IntegrityCore.context = context
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

    fun markErrorsRead(context: Context) {
        android.util.Log.v("IntegrityCore", "Errors marked read")
        logRepository.markAllRead()
        ErrorNotifier.removeNotification(context)
    }

    fun updateScheduledJobsOptions(jobsEnabled: Boolean) {
        settingsRepository.set(settingsRepository.get()
                .copy(jobsEnableScheduled = jobsEnabled))
    }

    private fun notifyAboutDisabledScheduledJobs(context: Context) {
        val showDisabledScheduledJobsNotification = !scheduledJobsEnabled()
                && settingsRepository.get().notificationShowDisabledScheduled
        if (showDisabledScheduledJobsNotification) {
            DisabledScheduledJobsNotifier.showNotification(context)
        } else {
            DisabledScheduledJobsNotifier.removeNotification(context)
        }
    }

    fun scheduledJobsEnabled() = settingsRepository.get().jobsEnableScheduled

    fun getDataFolderName() = settingsRepository.get().dataFolderPath

    fun getSortingMethod() = settingsRepository.get().sortingMethod

    fun getColors() = with(settingsRepository.get()) {
        ThemeColors(colorBackground, colorPrimary, colorAccent)
    }

    fun getFont() = settingsRepository.get().textFont

}