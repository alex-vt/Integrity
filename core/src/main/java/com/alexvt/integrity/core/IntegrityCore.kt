/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.core.database.MetadataRepository
import com.alexvt.integrity.core.database.SimplePersistableMetadataRepository
import com.alexvt.integrity.core.filesystem.ArchiveLocationUtil
import com.alexvt.integrity.core.filesystem.FolderLocationRepository
import com.alexvt.integrity.core.filesystem.SimplePersistableFolderLocationRepository
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.job.ScheduledJobManager
import com.alexvt.integrity.core.type.DataTypeUtil
import com.alexvt.integrity.core.util.ArchiveUtil
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.alexvt.integrity.core.util.HashUtil
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import kotlin.coroutines.CoroutineContext

@SuppressLint("StaticFieldLeak") // context // todo DI
/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
object IntegrityCore {

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
        metadataRepository.getAllArtifactMetadata().snapshotMetadataList
                .filter { it.status == SnapshotStatus.IN_PROGRESS }
                .forEach {
                    metadataRepository.removeSnapshotMetadata(it.artifactId, it.date)
                    metadataRepository.addSnapshotMetadata(it.copy(status = SnapshotStatus.INCOMPLETE))
                }
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

    fun getNextJobRunTimestamp(snapshotMetadata: SnapshotMetadata)
            = ScheduledJobManager.getNextRunTimestamp(snapshotMetadata)

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

    /**
     * Saves preliminary (intended) snapshot metadata to database. No data is processed here.
     */
    fun saveSnapshotBlueprint(snapshotMetadata: SnapshotMetadata): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(System.currentTimeMillis())
        metadataRepository.cleanupArtifactBlueprints(snapshotMetadata.artifactId) // no old ones
        metadataRepository.addSnapshotMetadata(snapshotMetadata.copy(
                date = date,
                status = SnapshotStatus.BLUEPRINT
        ))
        ScheduledJobManager.updateSchedule()
        return date
    }

    /**
     * Creates a snapshot of an existing artifact:
     *
     * Obtains metadata snapshot with the given artifact ID and date from the database,
     * uses it as a blueprint for the new snapshot,
     * downloads corresponding data,
     * writes its (new) metadata to database
     * and writes data to archive according to the known archive path from metadata.
     *
     * Returns metadata of the newly created snapshot
     * (preliminary metadata on job start, then the final metadata when job complete).
     */
    fun createSnapshotFromBlueprint(blueprintSnapshotArtifactId: Long,
                                    blueprintSnapshotDate: String) {
        val metadataInProgress = metadataRepository
                .getSnapshotMetadata(blueprintSnapshotArtifactId, blueprintSnapshotDate)
                .copy(status = SnapshotStatus.IN_PROGRESS)

        // the corresponding incomplete snapshot (if any) no longer needed
        // as snapshot now is being created
        metadataRepository.removeSnapshotMetadata(metadataInProgress.artifactId,
                metadataInProgress.date)
        metadataRepository.addSnapshotMetadata(metadataInProgress)

        val coroutineJob = GlobalScope.launch (Dispatchers.Default) {
            createSnapshot(metadataInProgress, coroutineContext)
        }

        RunningJobManager.addJob(metadataInProgress, coroutineJob)
    }

    fun subscribeToJobProgress(artifactId: Long, date: String,
                               jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit) {
        val snapshotMetadataInProgress = metadataRepository.getSnapshotMetadata(artifactId, date)
        RunningJobManager.setJobProgressListener(snapshotMetadataInProgress, jobProgressListener)
    }

    /**
     * Cancels long running job if it's running. Metadata status changes to Incomplete.
     */
    fun cancelSnapshotCreation(artifactId: Long, date: String) {
        val snapshotMetadataInProgress = metadataRepository.getSnapshotMetadata(artifactId, date)
        RunningJobManager.removeJob(snapshotMetadataInProgress)

        val incompleteMetadata = snapshotMetadataInProgress.copy(status = SnapshotStatus.INCOMPLETE)
        metadataRepository.removeSnapshotMetadata(incompleteMetadata.artifactId,
                incompleteMetadata.date)
        metadataRepository.addSnapshotMetadata(incompleteMetadata)
        ScheduledJobManager.updateSchedule()
    }

    /**
     * Checks if data for snapshot defined by artifactId and date is in data cache folder.
     * If not, fetches archive according to metadata from database
     * and unpacks it to data cache folder
     */
    fun fetchSnapshotData(artifactId: Long, date: String): String {
        // todo
        return DataCacheFolderUtil.getSnapshotFolderPath(artifactId, date)
    }

    /**
     * Returns intent for (starting) previewing and creating new snapshot
     * for artifact defined by artifactId.
     */
    fun getSnapshotCreateIntent(context: Context, artifactId: Long): Intent {
        val snapshotMetadata = metadataRepository.getLatestSnapshotMetadata(artifactId)
        val activityClass = getDataTypeUtil(snapshotMetadata.dataTypeSpecificMetadata)
                .getOperationMainActivityClass()
        val typeViewIntent = Intent(context, activityClass)
        typeViewIntent.putExtra("artifactId", artifactId)
        return typeViewIntent
    }

    /**
     * Returns intent for (starting) viewing data for existing snapshot
     * defined by artifactId and date.
     */
    fun getSnapshotViewIntent(context: Context, artifactId: Long, date: String): Intent {
        val snapshotMetadata = metadataRepository.getSnapshotMetadata(artifactId, date)
        val activityClass = getDataTypeUtil(snapshotMetadata.dataTypeSpecificMetadata)
                .getOperationMainActivityClass()
        val typeViewCreateIntent = Intent(context, activityClass)
        typeViewCreateIntent.putExtra("artifactId", artifactId)
        typeViewCreateIntent.putExtra("date", date)
        return typeViewCreateIntent
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
        DataCacheFolderUtil.clear(artifactId)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean) {
        metadataRepository.removeSnapshotMetadata(artifactId, date)
        DataCacheFolderUtil.clear(artifactId, date)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeAll(alsoRemoveData: Boolean) {
        metadataRepository.clear()
        DataCacheFolderUtil.clear()
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

    fun <T: TypeMetadata> registerDataTypeUtil(dataType: Class<T>,
                                               dataTypeUtil: DataTypeUtil<T>) {
        dataTypeUtilMap[dataType.simpleName] = dataTypeUtil
    }

    /**
     * Gets alphabetically sorted map of names of registered data types
     * to intents of main view activities for these data types.
     */
    fun getNamedArtifactCreateIntentMap(context: Context): Map<String, Intent>
            = dataTypeUtilMap.map { it -> Pair(it.value.getTypeScreenName(),
            Intent(context, it.value.getOperationMainActivityClass())) }
            .toMap()
            .toSortedMap()

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
     * Gets alphabetically sorted map of names of archive locations saved by user
     * to the archive locations themselves.
     */
    fun getNamedFolderLocationMap()
            = getNamedFolderLocationMap(folderLocationRepository.getAllFolderLocations())

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

    private val dataTypeUtilMap: MutableMap<String, DataTypeUtil<*>> = HashMap()

    /**
     * See https://stackoverflow.com/a/41103379
     */
    private fun <F: FolderLocation> getFileLocationUtil(dataArchiveLocation: F): ArchiveLocationUtil<F> {
        val fileLocationUtil = archiveLocationUtilMap[dataArchiveLocation.javaClass.simpleName]!!
        return (fileLocationUtil as ArchiveLocationUtil<F>)
    }

    private fun <T: TypeMetadata> getDataTypeUtil(typeMetadata: T): DataTypeUtil<T> {
        val dataTypeUtil = dataTypeUtilMap[typeMetadata.javaClass.simpleName]!!
        return (dataTypeUtil as DataTypeUtil<T>)
    }

    /**
     * Downloads type specific data,
     * archives it and the given metadata to cache,
     * then writes archive and its hash sum file to locations according to metadata,
     * saves metadata to database,
     * clears unneeded files (archives and hashes) from cache while keeping snapshot folders,
     * returns metadata.
     *
     * Intended metadata as a blueprint is saved to database at once
     * to keep track of possibly interrupted job.
     * The blueprint is overwritten by the complete metadata after job succeeds.
     */
    private suspend fun createSnapshot(metadataInProgress: SnapshotMetadata,
                                       jobCoroutineContext: CoroutineContext) {
        postProgress(metadataInProgress, "Downloading data")
        val dataFolderPath = getDataTypeUtil(metadataInProgress.dataTypeSpecificMetadata)
                .downloadData(metadataInProgress.artifactId,
                        metadataInProgress.date,
                        metadataInProgress.dataTypeSpecificMetadata,
                        jobCoroutineContext)
        if (!jobCoroutineContext.isActive) {
            return
        }

        postProgress(metadataInProgress, "Compressing data")

        // Switching over to complete metadata to archive with data.
        // Note: starting from here existing data will be overwritten
        // even if partially written before.
        val completeMetadata = metadataInProgress.copy(status = SnapshotStatus.COMPLETE)

        val archivePath = ArchiveUtil.archiveFolderAndMetadata(dataFolderPath, completeMetadata)
        val archiveHashPath = "$archivePath.sha1"
        DataCacheFolderUtil.writeTextToFile(HashUtil.getFileHash(archivePath), archiveHashPath)

        completeMetadata.archiveFolderLocations.forEachIndexed { index, dataArchiveLocation -> run {
            if (!jobCoroutineContext.isActive) {
                return
            }
            postProgress(completeMetadata, "Saving archive to " + dataArchiveLocation + " "
                    + (index + 1) + " of " + completeMetadata.archiveFolderLocations.size)
            getFileLocationUtil(dataArchiveLocation).writeArchive(
                    sourceArchivePath = archivePath,
                    sourceHashPath = archiveHashPath,
                    artifactId = completeMetadata.artifactId,
                    artifactAlias = getArtifactAlias(completeMetadata.title),
                    date = completeMetadata.date,
                    archiveFolderLocation = dataArchiveLocation
            )
        } }
        if (!jobCoroutineContext.isActive) {
            return
        }

        postProgress(completeMetadata, "Saving metadata to database")
        // finally replacing incomplete metadata with complete one in database
        metadataRepository.removeSnapshotMetadata(metadataInProgress.artifactId,
                metadataInProgress.date)
        metadataRepository.addSnapshotMetadata(completeMetadata)
        DataCacheFolderUtil.clearFiles() // folders remain

        if (jobCoroutineContext.isActive) {
            postProgress(completeMetadata, "Done")
            delay(800)
            postResult(completeMetadata)
        }
        RunningJobManager.removeJob(completeMetadata)
        ScheduledJobManager.updateSchedule()
    }

    fun postProgress(artifactId: Long, date: String, message: String) {
        postProgress(metadataRepository.getSnapshotMetadata(artifactId, date), message)
    }

    private fun postProgress(snapshotMetadata: SnapshotMetadata, message: String) {
        Log.d("IntegrityCore", "Job progress: " + message)
        GlobalScope.launch(Dispatchers.Main) {
            RunningJobManager.invokeJobProgressListener(snapshotMetadata, JobProgress(
                    progressMessage = message
            ))
        }
    }

    private fun postResult(result: SnapshotMetadata) {
        Log.d("IntegrityCore", "Job result: " + result)
        GlobalScope.launch(Dispatchers.Main) {
            RunningJobManager.invokeJobProgressListener(result, JobProgress(
                    result = result
            ))
        }
    }

    /**
     * Makes a short filepath friendly name from artifact title,
     * like: Artifact, Name -> artifact_name
     */
    private fun getArtifactAlias(title: String): String {
        return Regex("[^A-Za-z0-9]+").replace(title, "_")
                .take(20)
                .trim('_')
                .toLowerCase()
    }
}