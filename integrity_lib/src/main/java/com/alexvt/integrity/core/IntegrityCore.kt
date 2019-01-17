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
import com.alexvt.integrity.core.type.SnapshotDownloadCancelRequest
import com.alexvt.integrity.core.type.SnapshotDownloadStartRequest
import com.alexvt.integrity.core.util.*
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.MetadataCollection
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import com.alexvt.integrity.lib.util.IntentUtil


@SuppressLint("StaticFieldLeak") // context // todo DI
/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
object IntegrityCore {

    // todo use content providers
    lateinit var metadataRepository: MetadataRepository
    lateinit var folderLocationRepository: FolderLocationRepository

    lateinit var context: Context

    /**
     * Should be called before using any other functions.
     */
    fun init(context: Context) {
        IntegrityCore.context = context
        metadataRepository = SimplePersistableMetadataRepository // todo replace with database
        metadataRepository.init(context)
        folderLocationRepository = SimplePersistableFolderLocationRepository // todo replace with database
        folderLocationRepository.init(context)
        resetInProgressSnapshotStatuses() // if there are any in progress snapshots, they are rogue
        ScheduledJobManager.updateSchedule()
    }

    private fun resetInProgressSnapshotStatuses() {
        metadataRepository.getAllArtifactMetadata().snapshots
                .filter { it.status == SnapshotStatus.IN_PROGRESS }
                .forEach {
                    metadataRepository.removeSnapshotMetadata(it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(it.copy(status = SnapshotStatus.INCOMPLETE))
                }
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
        IntentUtil.putFolderLocationNames(intent, getFolderNames(snapshot))
        activity.startActivityForResult(intent, 0)
    }

    fun openCreateNewSnapshot(activity: Activity, artifactId: Long) {
        val snapshot = metadataRepository.getLatestSnapshotMetadata(artifactId)
        val activityInfo = getDataTypeActivityInfo(snapshot.dataTypeClassName)
        val intent = Intent()
        intent.component = ComponentName(activityInfo.packageName, activityInfo.name)
        IntentUtil.putSnapshot(intent, snapshot.copy(status = SnapshotStatus.BLUEPRINT)) // as blueprint
        IntentUtil.putFolderLocationNames(intent, getFolderNames(snapshot))
        activity.startActivityForResult(intent, 0)
    }

    fun getFolderNames(snapshot: Snapshot) = snapshot.archiveFolderLocations
            .map { IntegrityCore.getFolderLocationName(it) }
            .toTypedArray()

    fun openCreateNewArtifact(activity: Activity, componentName: ComponentName) {
        val intent = Intent()
        intent.component = componentName
        IntentUtil.putFolderLocationNames(intent, emptyArray())
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
        ScheduledJobManager.updateSchedule()
    }

    /**
     * Returns intent for editing archive location defined by title.
     */
    fun getFolderLocationEditIntent(context: Context, title: String): Intent {
        val folderLocation = folderLocationRepository.getAllFolderLocations()
                .first { it.title == title }
        val activityClass = getFileLocationUtil(folderLocation).getViewMainActivityClass()
        val typeViewCreateIntent = Intent(context, activityClass)
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

    fun <F: FolderLocation> registerFileLocationUtil(fileLocation: Class<F>,
                                                                              archiveLocationUtil: ArchiveLocationUtil<F>) {
        archiveLocationUtilMap[fileLocation.simpleName] = archiveLocationUtil
    }

    /**
     * Gets alphabetically sorted set of names of available data types.
     */
    fun getTypeNames() = getDataTypeActivityInfoList()
            .map { ComponentName(it.packageName, it.name) }
            .sortedBy { it.className.substringAfterLast(".") } // sorted by simple name

    /**
     * Gets alphabetically sorted map of labels of registered folder location types
     * to intents of main view activities for these folder location types.
     */
    fun getNamedFileLocationCreateIntentMap(context: Context): Map<String, Intent>
            = archiveLocationUtilMap.map { it -> Pair(it.value.getFolderLocationLabel(),
            Intent(context, it.value.getViewMainActivityClass())) }
            .toMap()
            .toSortedMap()

    /**
     * Gets alphabetically sorted map of given archive location names
     * to the archive locations themselves.
     */
    fun getNamedFolderLocationMap(folderLocations: Collection<FolderLocation>)
            = folderLocations
            .map { it -> Pair(getFolderLocationName(it), it) }
            .toMap()
            .toSortedMap()

    fun getFolderLocationName(folderLocation: FolderLocation) = folderLocation.title +
            " (" + getFileLocationUtil(folderLocation).getFolderLocationLabel() + "): " +
            getFileLocationUtil(folderLocation).getFolderLocationDescription(folderLocation)

    private val archiveLocationUtilMap: MutableMap<String, ArchiveLocationUtil<*>> = HashMap()

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> getFileLocationUtil(dataArchiveLocation: F): ArchiveLocationUtil<F> {
        val fileLocationUtil = archiveLocationUtilMap[dataArchiveLocation.javaClass.simpleName]!!
        return (fileLocationUtil as ArchiveLocationUtil<F>)
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

}