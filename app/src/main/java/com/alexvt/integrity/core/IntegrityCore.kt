/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.database.MetadataRepository
import com.alexvt.integrity.core.database.SimplePersistableMetadataRepository
import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.core.credentials.SimplePersistableCredentialsRepository
import com.alexvt.integrity.core.job.ScheduledJobManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import com.alexvt.integrity.core.destinations.DestinationUtil
import com.alexvt.integrity.core.destinations.local.LocalFolderLocation
import com.alexvt.integrity.core.destinations.samba.SambaFolderLocation
import com.alexvt.integrity.core.log.*
import com.alexvt.integrity.core.notification.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.notification.ErrorNotifier
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.search.SimplePersistableSearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.core.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.core.util.*
import com.alexvt.integrity.lib.*

@SuppressLint("StaticFieldLeak") // context // todo DI
/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
object IntegrityCore {
    // todo replace early implementations
    val metadataRepository: MetadataRepository = SimplePersistableMetadataRepository
    val credentialsRepository: CredentialsRepository = SimplePersistableCredentialsRepository
    val searchIndexRepository: SearchIndexRepository = SimplePersistableSearchIndexRepository
    val logRepository: LogRepository = SimplePersistableLogRepository
    val settingsRepository: SettingsRepository = SimplePersistableSettingsRepository

    lateinit var context: Context

    /**
     * Should be called before using any other functions.
     */
    fun init(context: Context) {
        IntegrityCore.context = context
        logRepository.init(context)
        settingsRepository.init(context)
        metadataRepository.init(context)
        credentialsRepository.init(context)
        searchIndexRepository.init(context)

        resetInProgressSnapshotStatuses() // if there are any in progress snapshots, they are rogue

        settingsRepository.addChangesListener(this.toString()) {
            notifyAboutDisabledScheduledJobs(context)
            ScheduledJobManager.updateSchedule(context)
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
                    metadataRepository.removeSnapshotMetadata(context, it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(context,
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
        logRepository.markAllRead(context)
        ErrorNotifier.removeNotification(context)
    }

    fun updateScheduledJobsOptions(context: Context, jobsEnabled: Boolean) {
        settingsRepository.set(context, settingsRepository.get()
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

    fun getCompleteSnapshotDatesOrNull(artifactId: Long) = metadataRepository
            .getArtifactMetadata(artifactId).snapshots
            .filter { it.status == SnapshotStatus.COMPLETE }
            .map { it.date }
            .reversed() // in ascending order
            .ifEmpty { null }

    fun getColors() = with(settingsRepository.get()) {
        ThemeColors(colorBackground, colorPrimary, colorAccent)
    }

    fun getFont() = settingsRepository.get().textFont

    fun getColorBackground() = settingsRepository.get().colorBackground

    fun getColorPrimary() = settingsRepository.get().colorPrimary

    fun getColorAccent() = settingsRepository.get().colorAccent


    fun getDestinationComponent(title: String): ComponentName {
        val folderLocation = settingsRepository.getAllFolderLocations()
                .first { it.title == title }
        return getDestinationUtil(folderLocation.javaClass)
                .getViewMainActivityComponent()
    }


    /**
     * Gets alphabetically sorted set of names of available data types.
     */
    fun getTypeNames() = getDataTypeActivityInfoList()
            .map { ComponentName(it.packageName, it.name) }
            .sortedBy { it.className.substringAfterLast(".") } // sorted by simple name

    /**
     * Gets list of archive destination labels.
     */
    fun getDestinationNames() = getDestinationClasses().map {
        IntegrityEx.getDestinationNameUtil(it).getFolderLocationLabel()
    }

    fun getDestinationClasses() = listOf(
            LocalFolderLocation::class.java,
            SambaFolderLocation::class.java
    )

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> getDestinationUtil(dataArchiveLocationClass: Class<F>): DestinationUtil<F> {
        val utilClassName = dataArchiveLocationClass.name
                .replace("FolderLocation", "DestinationUtil")
        return Class.forName(utilClassName).kotlin.objectInstance as DestinationUtil<F>
    }

    fun getDataTypeActivityInfo(typeClassName: String): ActivityInfo {
        val activityInfoList = getDataTypeActivityInfoList()
        return activityInfoList.first {
            typeClassName.substringAfterLast(".").removeSuffix("Metadata") ==
                    it.name.substringAfterLast(".").removeSuffix("Activity")
        }
    }

    private fun getDataTypeActivityInfoList() =
            context.packageManager.queryIntentActivities(
                    Intent("com.alexvt.integrity.ACTION_VIEW"), 0)
                    .filter { it?.activityInfo != null }
                    .map { it.activityInfo }

}