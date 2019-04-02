/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.snapshots

import com.alexvt.integrity.core.data.device.DeviceInfoRepository
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.data.search.SearchIndexRepository
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.operations.destinations.DestinationUtilManager
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.data.jobs.JobProgress
import com.alexvt.integrity.lib.core.data.jobs.RunningJobRepository
import com.alexvt.integrity.lib.core.operations.log.Log
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.metadata.SnapshotStatus
import com.alexvt.integrity.lib.core.data.search.DataChunks
import com.alexvt.integrity.lib.core.operations.log.LogManager
import com.alexvt.integrity.lib.core.util.JsonSerializerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

/**
 * Saves, interrupts saving or removes snapshot metadata and data.
 */
abstract class SnapshotOperationManager(
        private val metadataRepository: MetadataRepository,
        private val searchIndexRepository: SearchIndexRepository,
        private val dataFolderManager: DataFolderManager,
        private val runningJobRepository: RunningJobRepository,
        private val settingsRepository: SettingsRepository,
        private val archiveManager: ArchiveManager,
        private val deviceInfoRepository: DeviceInfoRepository,
        private val logManager: LogManager,
        private val destinationUtilManager: DestinationUtilManager
) {

    /**
     * Saves snapshot data and/or metadata blueprint according to its status.
     * @return true when snapshot is saving
     */
    fun saveSnapshot(snapshot: Snapshot): Boolean {
        if (snapshot.status == SnapshotStatus.BLUEPRINT
                || snapshot.status == SnapshotStatus.IN_PROGRESS) {
            saveSnapshotBlueprint(snapshot)
        }
        if (snapshot.status == SnapshotStatus.IN_PROGRESS
                || snapshot.status == SnapshotStatus.INCOMPLETE) {
            val snapshotBlueprint = metadataRepository
                    .getLatestSnapshotMetadataBlocking(snapshot.artifactId)
            if (deviceStateAllowsDownload(snapshot)) {
                downloadFromBlueprint(snapshotBlueprint)
                return true
            }
        }
        return false
    }

    /**
     * Cancels long running job if it's running. Metadata status changes to Incomplete.
     */
    fun cancelSnapshotCreation(artifactId: Long, date: String) {
        val snapshotInProgress = metadataRepository.getSnapshotMetadataBlocking(artifactId,
                date)
        terminateDownload(snapshotInProgress)

        // Updating snapshot status as incomplete in database.
        val incompleteMetadata = snapshotInProgress.copy(status = SnapshotStatus.INCOMPLETE)
        metadataRepository.removeSnapshotMetadata(
                incompleteMetadata.artifactId, incompleteMetadata.date)
        metadataRepository.addSnapshotMetadata(incompleteMetadata)
    }

    abstract fun terminateDownload(snapshot: Snapshot)

    /**
     * Removes artifact specified by artifact ID, with all its snapshots metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeArtifact(artifactId: Long, alsoRemoveData: Boolean) {
        metadataRepository.removeArtifactMetadata(artifactId)
        searchIndexRepository.removeForArtifact(artifactId)
        dataFolderManager.clear(getDataFolderPath(), artifactId)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean) {
        metadataRepository.removeSnapshotMetadata(artifactId, date)
        searchIndexRepository.removeForSnapshot(artifactId, date)
        dataFolderManager.clear(getDataFolderPath(), artifactId, date)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeAllSnapshots(alsoRemoveData: Boolean) {
        metadataRepository.clear()
        searchIndexRepository.clear()
        dataFolderManager.clear(getDataFolderPath())
        // todo alsoRemoveData if needed
    }


    private fun deviceStateAllowsDownload(snapshot: Snapshot): Boolean {
        val batteryStateForbidsDownload = !snapshot.downloadSchedule.allowOnLowBattery
                && !deviceInfoRepository.isBatteryChargeMoreThan(20) // todo setting
        if (batteryStateForbidsDownload) {
            Log(logManager, "Battery charge is too low to download snapshot ${snapshot.title}")
                    .logError()
            return false
        }
        val wifiStateForbidsDownload = snapshot.downloadSchedule.allowOnWifiOnly
                && !deviceInfoRepository.isOnWifi()
        if (wifiStateForbidsDownload) {
            Log(logManager, "WiFi connection is needed to download snapshot ${snapshot.title}")
                    .logError()
            return false
        }
        return true
    }

    /**
     * Saves preliminary (intended) snapshot metadata to database. No data is processed here.
     */
    private fun saveSnapshotBlueprint(snapshot: Snapshot): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(System.currentTimeMillis())
        metadataRepository.cleanupArtifactBlueprints(snapshot.artifactId) // no old ones
        metadataRepository.addSnapshotMetadata(snapshot.copy(
                date = date,
                status = SnapshotStatus.BLUEPRINT
        ))
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
    private fun downloadFromBlueprint(blueprintSnapshot: Snapshot) {
        // overwriting the previous snapshot state in database with In Progress
        val snapshotInProgress = blueprintSnapshot.copy(status = SnapshotStatus.IN_PROGRESS)
        metadataRepository.removeSnapshotMetadata(
                snapshotInProgress.artifactId, snapshotInProgress.date)
        metadataRepository.addSnapshotMetadata(snapshotInProgress)

        // starting download
        runningJobRepository.putJob(snapshotInProgress)
        postSnapshotDownloadProgress(snapshotInProgress, "Downloading data")

        dataFolderManager.ensureSnapshotFolder(getDataFolderPath(),
                snapshotInProgress.artifactId, snapshotInProgress.date)
        startSnapshotDataTypeDownloader(snapshotInProgress)
    }

    /**
     * Call continueSavingSnapshot after snapshot data download complete.
     */
    abstract fun startSnapshotDataTypeDownloader(snapshotInProgress: Snapshot)

    companion object {
        fun continueSavingSnapshot(
                snapshotOperationManager: SnapshotOperationManager,
                metadataRepository: MetadataRepository,
                artifactId: Long,
                date: String,
                isDownloaded: Boolean,
                message: String
        ) = GlobalScope.launch(Dispatchers.Default) {
            val snapshot = metadataRepository.getSnapshotMetadataBlocking(artifactId, date)
            if (isDownloaded) {
                snapshotOperationManager.archiveSnapshot(snapshot)
            } else {
                snapshotOperationManager.postSnapshotDownloadProgress(snapshot, message)
            }
        }
    }

    /**
     * Packs snapshot data and metadata,
     * then sends archives to folder locations according to metadata.
     */
    fun archiveSnapshot(snapshotInProgress: Snapshot) {
        if (!runningJobRepository.isRunning(snapshotInProgress)) {
            return
        }

        // Packing downloaded snapshot
        val dataFolderPath = dataFolderManager.ensureSnapshotFolder(
                getDataFolderPath(), snapshotInProgress.artifactId,
                snapshotInProgress.date)
        postSnapshotDownloadProgress(snapshotInProgress, "Compressing data")
        val archivePath = archiveManager.packSnapshot(dataFolderPath)
        val archiveHashPath = "$archivePath.sha1"
        dataFolderManager.writeTextToFile(HashUtil.getFileHash(archivePath), archiveHashPath)

        // Switching over to complete metadata to archive with data.
        // Note: starting from here existing data will be overwritten
        // even if partially written before.
        val completeSnapshot = snapshotInProgress.copy(status = SnapshotStatus.COMPLETE)

        completeSnapshot.archiveFolderLocations.forEachIndexed { index, dataArchiveLocation ->
            run {
                if (!runningJobRepository.isRunning(completeSnapshot)) {
                    return
                }
                postSnapshotDownloadProgress(completeSnapshot,
                        "Saving archive to " + dataArchiveLocation + " "
                                + (index + 1) + " of " + completeSnapshot.archiveFolderLocations.size)
                destinationUtilManager.get(dataArchiveLocation.javaClass).writeArchive(
                        sourceArchivePath = archivePath,
                        sourceHashPath = archiveHashPath,
                        artifactId = completeSnapshot.artifactId,
                        artifactAlias = getArtifactAlias(completeSnapshot.title),
                        date = completeSnapshot.date,
                        archiveFolderLocation = dataArchiveLocation
                )
            }
        }
        if (!runningJobRepository.isRunning(completeSnapshot)) {
            return
        }

        postSnapshotDownloadProgress(completeSnapshot, "Saving metadata to database")
        // finally replacing incomplete metadata with complete one in database
        metadataRepository.removeSnapshotMetadata(
                snapshotInProgress.artifactId, snapshotInProgress.date)
        metadataRepository.addSnapshotMetadata(completeSnapshot)
        dataFolderManager.clearFiles(getDataFolderPath()) // folders remain
        if (!runningJobRepository.isRunning(completeSnapshot)) {
            return
        }

        if (dataFolderManager.fileExists(dataFolderManager.getSnapshotDataChunksPath(
                        getDataFolderPath(), completeSnapshot.artifactId, completeSnapshot.date))) {
            postSnapshotDownloadProgress(completeSnapshot, "Updating search index")
            val dataChunkListJson = dataFolderManager.readTextFromFile(
                    dataFolderManager.getSnapshotDataChunksPath(getDataFolderPath(),
                            completeSnapshot.artifactId, completeSnapshot.date))
            val dataChunks = JsonSerializerUtil.fromJson("{\"chunks\": [$dataChunkListJson]}",
                    DataChunks::class.java).chunks // todo make clean, ensure no duplicates
            searchIndexRepository.add(dataChunks)
        }

        if (runningJobRepository.isRunning(completeSnapshot)) {
            postSnapshotDownloadProgress(completeSnapshot, "Done")
            postSnapshotDownloadResult(completeSnapshot)
        }
    }

    fun postSnapshotDownloadProgress(snapshot: Snapshot, message: String) {
        logProgress(message, snapshot)
        GlobalScope.launch(Dispatchers.Main) {
            runningJobRepository.invokeJobProgressListener(snapshot, JobProgress(
                    progressMessage = message
            ))
        }
    }


    // utility methods

    protected fun getDataFolderPath() = settingsRepository.get().dataFolderPath

    private fun postSnapshotDownloadResult(result: Snapshot) {
        logProgress("Snapshot download complete", result)
        GlobalScope.launch(Dispatchers.Main) {
            runningJobRepository.invokeJobProgressListener(result, JobProgress(
                    result = result
            ))
            runningJobRepository.removeJob(result)
        }
    }

    private fun logProgress(text: String, snapshot: Snapshot) {
        Log(logManager, text).snapshot(snapshot).log()
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