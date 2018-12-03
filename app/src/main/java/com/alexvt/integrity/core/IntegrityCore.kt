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
import com.alexvt.integrity.core.job.LongRunningJobManager
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
     * Creates the first snapshot of a new artifact:
     *
     * Obtains a new user data snapshot at the time of calling,
     * according to provided typed content metadata, title and description,
     * writes snapshot metadata to database
     * and writes data to archive according to provided data archive path.
     *
     * Returns metadata of the created snapshot
     * (preliminary metadata on job start, then the final metadata when job complete).
     */
     fun createArtifact(title: String,
                               description: String,
                               dataArchiveLocations: ArrayList<FolderLocation>,
                               dataTypeSpecificMetadata: TypeMetadata,
                               jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit)
            : LongRunningJob<SnapshotMetadata> {
        val timestampMillis = System.currentTimeMillis()

        val snapshotMetadata = SnapshotMetadata(
                artifactId = timestampMillis,
                title = title,
                date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(timestampMillis),
                description = description,
                archiveFolderLocations = dataArchiveLocations,
                dataTypeSpecificMetadata = dataTypeSpecificMetadata
        )

        LongRunningJobManager.addJob(timestampMillis, GlobalScope.launch (Dispatchers.Main) {
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Creating the first snapshot"
            ))
            createSnapshot(snapshotMetadata, jobProgressListener, coroutineContext)
        })
        return LongRunningJob(timestampMillis, snapshotMetadata)
    }

    /**
     * Creates another snapshot of an existing artifact:
     *
     * Obtains the most recent metadata snapshot with the given artifact ID from the database,
     * obtains a new user data snapshot at the time of calling,
     * writes its (new) metadata to database
     * and writes data to archive according to the known archive path from metadata.
     *
     * Returns metadata of the newly created snapshot
     * (preliminary metadata on job start, then the final metadata when job complete).
     */
    fun createSnapshot(artifactId: Long,
                       jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit)
            : LongRunningJob<SnapshotMetadata> {
        val timestampMillis = System.currentTimeMillis()
        val newSnapshotDate = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(timestampMillis)

        val previousSnapshotMetadata = SimplePersistableMetadataRepository.getLatestSnapshotMetadata(artifactId)
        val newSnapshotMetadata = previousSnapshotMetadata.copy(date = newSnapshotDate)

        LongRunningJobManager.addJob(timestampMillis, GlobalScope.launch (Dispatchers.Main) {
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Creating a new snapshot"
            ))
            createSnapshot(newSnapshotMetadata, jobProgressListener, coroutineContext)
        })
        return LongRunningJob(timestampMillis, newSnapshotMetadata)
    }

    /**
     * Cancels long running job if it's running
     */
    fun cancelJob(longRunningJob: LongRunningJob<*>) {
        LongRunningJobManager.cancelJob(longRunningJob.id)
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
     */
    private suspend fun createSnapshot(snapshotMetadata: SnapshotMetadata,
                                       jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit,
                                       jobCoroutineContext: CoroutineContext): SnapshotMetadata {
        val dataFolderPath = getDataTypeUtil(snapshotMetadata.dataTypeSpecificMetadata)
                .downloadData(snapshotMetadata.artifactId,
                        snapshotMetadata.date,
                        snapshotMetadata.dataTypeSpecificMetadata,
                        jobProgressListener,
                        jobCoroutineContext)
        if (!jobCoroutineContext.isActive) {
            return snapshotMetadata
        }
        jobProgressListener.invoke(JobProgress(
                progressMessage = "Compressing data"
        ))
        val archivePath = ArchiveUtil.archiveFolderAndMetadata(dataFolderPath,
                snapshotMetadata)
        val archiveHashPath = "$archivePath.sha1"
        DataCacheFolderUtil.writeTextToFile(HashUtil.getFileHash(archivePath), archiveHashPath)

        snapshotMetadata.archiveFolderLocations.forEachIndexed { index, dataArchiveLocation -> run {
            if (!jobCoroutineContext.isActive) {
                return snapshotMetadata
            }
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Saving archive to " + dataArchiveLocation + " "
                            + (index + 1) + " of " + snapshotMetadata.archiveFolderLocations
            ))
            getFileLocationUtil(dataArchiveLocation).writeArchive(
                    sourceArchivePath = archivePath,
                    sourceHashPath = archiveHashPath,
                    artifactId = snapshotMetadata.artifactId,
                    artifactAlias = getArtifactAlias(snapshotMetadata.title),
                    date = snapshotMetadata.date,
                    archiveFolderLocation = dataArchiveLocation
            )
        } }

        if (!jobCoroutineContext.isActive) {
            return snapshotMetadata
        }
        jobProgressListener.invoke(JobProgress(
                progressMessage = "Saving metadata to database"
        ))

        SimplePersistableMetadataRepository.addSnapshotMetadata(snapshotMetadata)
        DataCacheFolderUtil.clearFiles() // folders remain

        if (jobCoroutineContext.isActive) {
            jobProgressListener.invoke(JobProgress(
                    progressMessage = "Done"
            ))
            delay(1000)
            jobProgressListener.invoke(JobProgress(
                    result = snapshotMetadata
            ))
        }
        LongRunningJobManager.removeJob(jobCoroutineContext)

        return snapshotMetadata
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