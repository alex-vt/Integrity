/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.core.database.MetadataRepository
import com.alexvt.integrity.core.database.SimplePersistableMetadataRepository
import com.alexvt.integrity.core.filesystem.ArchiveLocationUtil
import com.alexvt.integrity.core.filesystem.FolderLocationRepository
import com.alexvt.integrity.core.filesystem.SimplePersistableFolderLocationRepository
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.job.ScheduledJobManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import com.alexvt.integrity.core.filesystem.local.LocalFolderLocation
import com.alexvt.integrity.core.filesystem.samba.SambaFolderLocation
import com.alexvt.integrity.core.log.*
import com.alexvt.integrity.core.notification.ErrorNotifier
import com.alexvt.integrity.core.tags.SimplePersistableTagRepository
import com.alexvt.integrity.core.tags.TagRepository
import com.alexvt.integrity.core.type.SnapshotDownloadCancelRequest
import com.alexvt.integrity.core.util.*
import com.alexvt.integrity.lib.*
import com.alexvt.integrity.lib.util.IntentUtil


@SuppressLint("StaticFieldLeak") // context // todo DI
/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
object IntegrityCore {

    lateinit var metadataRepository: MetadataRepository
    lateinit var folderLocationRepository: FolderLocationRepository
    lateinit var tagRepository: TagRepository
    lateinit var logRepository: LogRepository

    lateinit var context: Context

    /**
     * Should be called before using any other functions.
     *
     * Initialization exceptions are caught and, if possible (log exists), logged.
     * After successful initialization uncaught exceptions will be logged (and app recovered).
     * This setup prevents "boot loops": as early as initialization fails, the app isn't recovered.
     */
    fun init(context: Context) {
        try {
            IntegrityCore.context = context
            logRepository = SimplePersistableLogRepository // todo replace with database
            logRepository.init(context)
            metadataRepository = SimplePersistableMetadataRepository // todo replace with database
            metadataRepository.init(context)
            folderLocationRepository = SimplePersistableFolderLocationRepository // todo replace with database
            folderLocationRepository.init(context)
            tagRepository = SimplePersistableTagRepository // todo replace with database
            tagRepository.init(context)

            resetInProgressSnapshotStatuses() // if there are any in progress snapshots, they are rogue
            ScheduledJobManager.updateSchedule(context)

            notifyAboutUnreadErrors(context)
            IntegrityEx.handleUncaughtExceptions(context)

            Log(context, "IntegrityCore initialized").log()
        } catch (t: Throwable) {
            try {
                Log(context, "Failed to start Integrity app").logError(t) // using logger
            } catch (t: Throwable) {
                val errorText = "Failed to start Integrity app (and its logs)"
                LoggingUtil.logErrorInLogcat(errorText, t)
                ErrorNotifier.notifyAboutFailure(context, errorText)
            }
        }
    }

    private fun resetInProgressSnapshotStatuses() {
        metadataRepository.getAllArtifactMetadata().snapshots
                .filter { it.status == SnapshotStatus.IN_PROGRESS }
                .forEach {
                    metadataRepository.removeSnapshotMetadata(it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(it.copy(status = SnapshotStatus.INCOMPLETE))
                }
    }

    fun notifyAboutUnreadErrors(context: Context) {
        val unreadErrors = logRepository.getUnreadErrors()
        if (unreadErrors.isNotEmpty()) {
            ErrorNotifier.notifyAboutErrors(context, unreadErrors)
        } else {
            ErrorNotifier.removeNotification(context)
        }
    }

    fun markErrorsRead(context: Context) {
        IntegrityCore.logRepository.markAllRead()
        ErrorNotifier.removeNotification(context)
    }

    fun openViewSnapshotOrShowProgress(activity: Activity, artifactId: Long, date: String) {
        val snapshot = metadataRepository.getSnapshotMetadata(artifactId, date)
        if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
            showRunningJobProgressDialog(activity, artifactId, date)
            return
        }
        // todo ensure snapshot data presence in folder first
        val activityInfo = getDataTypeActivityInfo(snapshot.dataTypeClassName)
        val intent = Intent()
        intent.component = ComponentName(activityInfo.packageName, activityInfo.name)
        IntentUtil.putSnapshot(intent, snapshot)
        if (snapshot.status == SnapshotStatus.COMPLETE) {
            IntentUtil.putDates(intent, getCompleteSnapshotDates(artifactId))
        }
        activity.startActivityForResult(intent, 0)
    }

    fun openCreateNewSnapshot(activity: Activity, artifactId: Long) {
        val snapshot = metadataRepository.getLatestSnapshotMetadata(artifactId)
        val activityInfo = getDataTypeActivityInfo(snapshot.dataTypeClassName)
        val intent = Intent()
        intent.component = ComponentName(activityInfo.packageName, activityInfo.name)
        IntentUtil.putSnapshot(intent, snapshot.copy(status = SnapshotStatus.BLUEPRINT)) // as blueprint
        activity.startActivityForResult(intent, 0)
    }

    private fun getCompleteSnapshotDates(artifactId: Long) = metadataRepository
            .getArtifactMetadata(artifactId).snapshots
            .filter { it.status == SnapshotStatus.COMPLETE }
            .map { it.date }
            .reversed() // in ascending order

    fun getFolderLocationNames(archiveFolderLocations: List<FolderLocation>) = archiveFolderLocations
            .map { IntegrityCore.getFolderLocationName(it) }
            .toTypedArray()

    fun openCreateNewArtifact(activity: Activity, componentName: ComponentName) {
        val intent = Intent()
        intent.component = componentName
        activity.startActivityForResult(intent, 0)
    }

    /**
     * Saves snapshot data and/or metadata blueprint according to its status.
     */
    fun saveSnapshot(context: Context, snapshot: Snapshot) {
        SnapshotSavingUtil.saveSnapshot(context, snapshot)
    }

    fun subscribeToRunningJobListing(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit) {
        RunningJobManager.addJobListListener(tag, jobsListener)
    }

    fun unsubscribeFromRunningJobListing(tag: String) {
        RunningJobManager.removeJobListListener(tag)
    }

    fun subscribeToScheduledJobListing(tag: String, jobsListener: (List<Pair<Long, String>>) -> Unit) {
        ScheduledJobManager.addScheduledJobsListener(tag, jobsListener)
    }

    fun unsubscribeFromScheduledJobListing(tag: String) {
        ScheduledJobManager.removeScheduledJobsListener(tag)
    }

    fun getNextJobRunTimestamp(snapshot: Snapshot)
            = ScheduledJobManager.getNextRunTimestamp(snapshot)

    fun showRunningJobProgressDialog(context: Context, artifactId: Long, date: String): MaterialDialog {
        val title = metadataRepository.getSnapshotMetadata(artifactId, date).title
        val progressDialog = MaterialDialog(context)
                .title(text = "Creating snapshot of $title")
                .cancelable(false)
                .positiveButton(text = "In background") {
                    it.cancel()
                }
        progressDialog.show()
        subscribeToJobProgress(artifactId, date) {
            progressDialog.message(text = it.progressMessage)
            if (it.result != null) {
                progressDialog.cancel()
            }
        }
        progressDialog.negativeButton(text = "Stop") {
            cancelSnapshotCreation(artifactId, date)
            it.cancel()
        }
        return progressDialog
    }

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
        SnapshotDownloadCancelRequest().send(context, snapshotInProgress)

        // Updating snapshot status as incomplete in database.
        val incompleteMetadata = snapshotInProgress.copy(status = SnapshotStatus.INCOMPLETE)
        metadataRepository.removeSnapshotMetadata(incompleteMetadata.artifactId,
                incompleteMetadata.date)
        metadataRepository.addSnapshotMetadata(incompleteMetadata)
        ScheduledJobManager.updateSchedule(context)
    }

    /**
     * Returns intent for editing archive location defined by title.
     */
    fun getFolderLocationEditIntent(title: String): Intent {
        val folderLocation = folderLocationRepository.getAllFolderLocations()
                .first { it.title == title }
        val typeViewCreateIntent = Intent()
        typeViewCreateIntent.component = getFileLocationUtil(folderLocation.javaClass)
                .getViewMainActivityComponent()
        typeViewCreateIntent.putExtra("title", title)
        return typeViewCreateIntent
    }

    /**
     * Removes artifact specified by artifact ID, with all its snapshots metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeArtifact(artifactId: Long, alsoRemoveData: Boolean) {
        metadataRepository.removeArtifactMetadata(artifactId)
        DataCacheFolderUtil.clear(context, artifactId)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean) {
        metadataRepository.removeSnapshotMetadata(artifactId, date)
        DataCacheFolderUtil.clear(context, artifactId, date)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeAll(alsoRemoveData: Boolean) {
        metadataRepository.clear()
        DataCacheFolderUtil.clear(context)
        // todo alsoRemoveData if needed
    }

    /**
     * Returns artifacts with only those snapshots data in which contains the provided query text.
     */
    fun searchText(queryText: String): List<MetadataCollection> {
        // todo
        return ArrayList()
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
            ).map { getFileLocationUtil(it) }
            .map { Pair(it.getFolderLocationLabel(), getNamedFileLocationCreateIntent(it)) }
            .toMap()
            .toSortedMap()

    private fun getNamedFileLocationCreateIntent(archiveLocationUtil: ArchiveLocationUtil<*>): Intent {
        val intent = Intent()
        intent.component = archiveLocationUtil.getViewMainActivityComponent()
        return intent
    }

    fun getFolderLocationName(folderLocation: FolderLocation) = folderLocation.title +
            " (" + getFileLocationUtil(folderLocation.javaClass).getFolderLocationLabel() + "): " +
            getFileLocationUtil(folderLocation.javaClass).getFolderLocationDescription(folderLocation)

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> getFileLocationUtil(dataArchiveLocationClass: Class<F>): ArchiveLocationUtil<F> {
        val utilClassName = dataArchiveLocationClass.name
                .replace("FolderLocation", "LocationUtil")
        return Class.forName(utilClassName).kotlin.objectInstance as ArchiveLocationUtil<F>
    }

    private fun getDataTypeActivityInfo(typeClassName: String): ActivityInfo {
        val activityInfoList = getDataTypeActivityInfoList()
        return activityInfoList.first {
            typeClassName.substringAfterLast(".").removeSuffix("Metadata") ==
                    it.name.substringAfterLast(".").removeSuffix("Activity")
        }
    }

    private fun getDataTypeActivityInfoList() =
            context.packageManager.queryIntentActivities(
                    Intent("com.alexvt.integrity.core.ACTION_VIEW"), 0)
                    .filter { it?.activityInfo != null }
                    .map { it.activityInfo }


    /**
     * TypeSpecificConverters
     */

    fun toTypeSpecificMetadata(snapshot: Snapshot) = SnapshotMetadata(
            artifactId = snapshot.artifactId,
            title = snapshot.title,
            date = snapshot.date,
            description = snapshot.description,
            downloadSchedule = snapshot.downloadSchedule,
            archiveFolderLocations = snapshot.archiveFolderLocations,
            tags = snapshot.tags,
            themeColor = snapshot.themeColor,
            dataTypeSpecificMetadata = JsonSerializerUtil.fromJson(
                    snapshot.dataTypeSpecificMetadataJson,
                    Class.forName(snapshot.dataTypeClassName) as Class<TypeMetadata>),
            status = snapshot.status
    )

    fun fromTypeSpecificMetadata(context: Context, snapshotMetadata: SnapshotMetadata) = Snapshot(
            artifactId = snapshotMetadata.artifactId,
            title = snapshotMetadata.title,
            date = snapshotMetadata.date,
            description = snapshotMetadata.description,
            downloadSchedule = snapshotMetadata.downloadSchedule,
            archiveFolderLocations = snapshotMetadata.archiveFolderLocations,
            tags = snapshotMetadata.tags,
            themeColor = snapshotMetadata.themeColor,
            dataTypeClassName = snapshotMetadata.dataTypeSpecificMetadata.javaClass.name,
            dataTypePackageName = context.packageName,
            dataTypeSpecificMetadataJson = JsonSerializerUtil.toJson(
                    snapshotMetadata.dataTypeSpecificMetadata),
            status = snapshotMetadata.status
    )
}