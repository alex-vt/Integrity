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
import com.alexvt.integrity.core.filesystem.ArchiveLocationUtil
import com.alexvt.integrity.core.filesystem.PresetRepository
import com.alexvt.integrity.core.filesystem.SimplePersistablePresetRepository
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.job.LongRunningJob
import com.alexvt.integrity.core.job.CoroutineJobManager
import com.alexvt.integrity.core.type.DataTypeUtil
import com.alexvt.integrity.core.util.ArchiveUtil
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.alexvt.integrity.core.util.HashUtil
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@SuppressLint("StaticFieldLeak") // context // todo DI
/**
 * The entry point of business logic, actions on data and metadata in the Integrity app
 */
object IntegrityCore {

    lateinit var metadataRepository: MetadataRepository
    lateinit var presetRepository: PresetRepository

    lateinit var context: Context

    /**
     * Should be called before using any other functions.
     */
    fun init(context: Context) {
        this.context = context
        metadataRepository = SimplePersistableMetadataRepository // todo replace with database
        metadataRepository.init(context)
        presetRepository = SimplePersistablePresetRepository // todo replace with database
        presetRepository.init(context)
    }

    /**
     * Saves preliminary (intended) snapshot metadata to database. No data is processed here.
     */
    fun saveSnapshotBlueprint(snapshotMetadata: SnapshotMetadata) {
        metadataRepository.cleanupArtifactBlueprints(snapshotMetadata.artifactId) // no old ones
        metadataRepository.addSnapshotMetadata(snapshotMetadata.copy(status = SnapshotStatus.BLUEPRINT))
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
                                    blueprintSnapshotDate: String,
                                    jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit)
            : LongRunningJob<SnapshotMetadata> {
        val metadataInProgress = SimplePersistableMetadataRepository
                .getSnapshotMetadata(blueprintSnapshotArtifactId, blueprintSnapshotDate)
                .copy(status = SnapshotStatus.INCOMPLETE)

        val coroutineJobId = CoroutineJobManager.addJob(GlobalScope.launch (Dispatchers.Main) {
            createSnapshot(metadataInProgress, jobProgressListener, coroutineContext)
        })
        return LongRunningJob(coroutineJobId, metadataInProgress)
    }

    /**
     * Cancels long running job if it's running
     */
    fun cancelJob(longRunningJob: LongRunningJob<*>) {
        CoroutineJobManager.cancelJob(longRunningJob.id)
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
        val snapshotMetadata = SimplePersistableMetadataRepository.getLatestSnapshotMetadata(artifactId)
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
        val snapshotMetadata = SimplePersistableMetadataRepository.getSnapshotMetadata(artifactId, date)
        val activityClass = getDataTypeUtil(snapshotMetadata.dataTypeSpecificMetadata)
                .getOperationMainActivityClass()
        val typeViewCreateIntent = Intent(context, activityClass)
        typeViewCreateIntent.putExtra("artifactId", artifactId)
        typeViewCreateIntent.putExtra("date", date)
        return typeViewCreateIntent
    }

    /**
     * Removes artifact specified by artifact ID, with all its snapshots metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeArtifact(artifactId: Long, alsoRemoveData: Boolean) {
        SimplePersistableMetadataRepository.removeArtifactMetadata(artifactId)
        DataCacheFolderUtil.clear(artifactId)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean) {
        SimplePersistableMetadataRepository.removeSnapshotMetadata(artifactId, date)
        DataCacheFolderUtil.clear(artifactId, date)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeAll(alsoRemoveData: Boolean) {
        SimplePersistableMetadataRepository.clear()
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
     * Gets alphabetically sorted map of given archive location names
     * to the archive locations themselves.
     */
    fun getNamedFolderLocationMap(folderLocations: Collection<FolderLocation>): Map<String, FolderLocation> {
        return folderLocations
                .map { it -> Pair(getFolderLocationName(it), it) }
                .toMap()
                .toSortedMap()
    }

    /**
     * Gets alphabetically sorted map of names of archive locations saved by user
     * to the archive locations themselves.
     */
    fun getNamedFolderLocationMap(): Map<String, FolderLocation> {
        return getNamedFolderLocationMap(presetRepository.getAllFolderLocations())
    }

    private fun getFolderLocationName(folderLocation: FolderLocation)
            = getFileLocationUtil(folderLocation).getFolderLocationTypeName() + " in " +
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
                                       jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
                                       jobCoroutineContext: CoroutineContext) {
        // the corresponding incomplete snapshot no longer needed as snapshot now is being created
        SimplePersistableMetadataRepository.removeSnapshotMetadata(metadataInProgress.artifactId,
                metadataInProgress.date)
        SimplePersistableMetadataRepository.addSnapshotMetadata(metadataInProgress)

        jobProgressListener.invoke(JobProgress(
                progressMessage = "Downloading data"
        ))
        // todo support skipping operations which are already done (interrupted the previous time)
        val dataFolderPath = getDataTypeUtil(metadataInProgress.dataTypeSpecificMetadata)
                .downloadData(metadataInProgress.artifactId,
                        metadataInProgress.date,
                        metadataInProgress.dataTypeSpecificMetadata,
                        jobProgressListener,
                        jobCoroutineContext)
        if (!jobCoroutineContext.isActive) {
            return
        }
        jobProgressListener.invoke(JobProgress(
                progressMessage = "Compressing data"
        ))

        val archivePath = ArchiveUtil.archiveFolderAndMetadata(dataFolderPath, metadataInProgress)
        val archiveHashPath = "$archivePath.sha1"
        DataCacheFolderUtil.writeTextToFile(HashUtil.getFileHash(archivePath), archiveHashPath)

        metadataInProgress.archiveFolderLocations.forEachIndexed { index, dataArchiveLocation -> run {
            if (!jobCoroutineContext.isActive) {
                return
            }
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Saving archive to " + dataArchiveLocation + " "
                            + (index + 1) + " of " + metadataInProgress.archiveFolderLocations
            ))
            getFileLocationUtil(dataArchiveLocation).writeArchive(
                    sourceArchivePath = archivePath,
                    sourceHashPath = archiveHashPath,
                    artifactId = metadataInProgress.artifactId,
                    artifactAlias = getArtifactAlias(metadataInProgress.title),
                    date = metadataInProgress.date,
                    archiveFolderLocation = dataArchiveLocation
            )
        } }
        if (!jobCoroutineContext.isActive) {
            return
        }

        val completeMetadata = metadataInProgress.copy(status = SnapshotStatus.COMPLETE)
        jobProgressListener.invoke(JobProgress(
                progressMessage = "Saving metadata to database"
        ))
        // replacing incomplete metadata with complete one in database
        SimplePersistableMetadataRepository.removeSnapshotMetadata(metadataInProgress.artifactId,
                metadataInProgress.date)
        SimplePersistableMetadataRepository.addSnapshotMetadata(completeMetadata)
        DataCacheFolderUtil.clearFiles() // folders remain

        if (jobCoroutineContext.isActive) {
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Done"
            ))
            delay(800)
            jobProgressListener.invoke(JobProgress(
                    result = completeMetadata
            ))
        }
        CoroutineJobManager.removeJob(jobCoroutineContext)
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