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
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.job.ScheduledJobManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import com.alexvt.integrity.core.destinations.DestinationUtil
import com.alexvt.integrity.core.destinations.ArchiveLocationUtil
import com.alexvt.integrity.core.destinations.local.LocalFolderLocation
import com.alexvt.integrity.core.destinations.samba.SambaFolderLocation
import com.alexvt.integrity.core.log.*
import com.alexvt.integrity.core.notification.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.notification.ErrorNotifier
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.search.SimplePersistableSearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.core.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.core.type.SnapshotDownloadCancelRequest
import com.alexvt.integrity.core.util.*
import com.alexvt.integrity.lib.*
import com.alexvt.integrity.lib.util.DataCacheFolderUtil

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

    /**
     * Saves snapshot data and/or metadata blueprint according to its status.
     */
    fun saveSnapshot(context: Context, snapshot: Snapshot)
            = SnapshotSavingUtil.saveSnapshot(context, snapshot)

    fun getNextJobRunTimestamp(snapshot: Snapshot)
            = ScheduledJobManager.getNextRunTimestamp(snapshot)

    fun subscribeToJobProgress(artifactId: Long, date: String,
                               jobProgressListener: (JobProgress<Snapshot>) -> Unit) {
        val snapshotInProgress = metadataRepository.getSnapshotMetadata(artifactId, date)
        RunningJobManager.setJobProgressListener(snapshotInProgress, jobProgressListener)
    }

    /**
     * Cancels long running job if it's running. Metadata status changes to Incomplete.
     */
    fun cancelSnapshotCreation(artifactId: Long, date: String) {
        val snapshotInProgress = metadataRepository.getSnapshotMetadata(artifactId, date)
        SnapshotDownloadCancelRequest().send(context, getDataFolderName(), snapshotInProgress)

        // Updating snapshot status as incomplete in database.
        val incompleteMetadata = snapshotInProgress.copy(status = SnapshotStatus.INCOMPLETE)
        metadataRepository.removeSnapshotMetadata(context, incompleteMetadata.artifactId,
                incompleteMetadata.date)
        metadataRepository.addSnapshotMetadata(context, incompleteMetadata)
    }

    /**
     * Returns intent for editing archive location defined by title.
     */
    fun getFolderLocationEditIntent(title: String): Intent {
        val folderLocation = settingsRepository.getAllFolderLocations()
                .first { it.title == title }
        val typeViewCreateIntent = Intent()
        typeViewCreateIntent.component = IntegrityEx.getFileLocationUtil(folderLocation.javaClass)
                .getViewMainActivityComponent()
        typeViewCreateIntent.putExtra("title", title)
        return typeViewCreateIntent
    }

    /**
     * Removes artifact specified by artifact ID, with all its snapshots metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeArtifact(artifactId: Long, alsoRemoveData: Boolean) {
        metadataRepository.removeArtifactMetadata(context, artifactId)
        searchIndexRepository.removeForArtifact(context, artifactId)
        DataCacheFolderUtil.clear(context, getDataFolderName(), artifactId)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean) {
        metadataRepository.removeSnapshotMetadata(context, artifactId, date)
        searchIndexRepository.removeForSnapshot(context, artifactId, date)
        DataCacheFolderUtil.clear(context, getDataFolderName(), artifactId, date)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeAll(alsoRemoveData: Boolean) {
        metadataRepository.clear(context)
        searchIndexRepository.clear(context)
        DataCacheFolderUtil.clear(context, getDataFolderName())
        // todo alsoRemoveData if needed
    }

    /**
     * Checks integrity of all stored and archived metadata, and also optionally the data.
     *
     * If data verification is not required,
     * hash of metadata in database is calculated and compared to given verified value,
     * and presence of correctly named data archives is checked according to metadata.
     *
     * If data verification is required,
     * then in addition to the previous steps
     * hash of every data archive is calculated and compared to the
     * value in snapshot metadata in database.
     */
    fun verifyIntegrity(metadataHash: String, alsoVerifyData: Boolean): Boolean {
        // todo
        return false
    }

    /**
     * Gets alphabetically sorted set of names of available data types.
     */
    fun getTypeNames() = getDataTypeActivityInfoList()
            .map { ComponentName(it.packageName, it.name) }
            .sortedBy { it.className.substringAfterLast(".") } // sorted by simple name

    /**
     * Gets alphabetically sorted map of labels of code listed folder location types
     * to intents of main view activities for these folder location types.
     */
    fun getNamedFileLocationCreateIntentMap(): Map<String, Intent>
            = listOf(
            LocalFolderLocation::class.java,
            SambaFolderLocation::class.java
            ).map { IntegrityEx.getFileLocationUtil(it) }
            .map { Pair(it.getFolderLocationLabel(), getNamedFileLocationCreateIntent(it)) }
            .toMap()
            .toSortedMap()

    private fun getNamedFileLocationCreateIntent(archiveLocationUtil: ArchiveLocationUtil<*>): Intent {
        val intent = Intent()
        intent.component = archiveLocationUtil.getViewMainActivityComponent()
        return intent
    }

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