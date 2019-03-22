/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.destinations.DestinationUtilResolver
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.lib.jobs.JobProgress
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.jobs.RunningJobManager
import com.alexvt.integrity.lib.search.DataChunks
import com.alexvt.integrity.lib.types.SnapshotDownloadCancelRequest
import com.alexvt.integrity.lib.types.SnapshotDownloadStartRequest
import com.alexvt.integrity.lib.util.DeviceStateUtil
import com.alexvt.integrity.lib.util.JsonSerializerUtil
import com.alexvt.integrity.lib.log.Log
import com.alexvt.integrity.lib.util.IntentUtil
import dagger.android.AndroidInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import javax.inject.Inject

/**
 * Saves snapshot metadata and data.
 */
class AndroidSnapshotOperationManager @Inject constructor(
        private val context: Context,
        private val metadataRepository: MetadataRepository,
        private val searchIndexRepository: SearchIndexRepository,
        private val dataFolderManager: DataFolderManager,
        private val runningJobManager: RunningJobManager,
        private val settingsRepository: SettingsRepository
) : SnapshotOperationManager {

    /**
     * Saves snapshot data and/or metadata blueprint according to its status.
     * @return true when snapshot is saving
     */
    override fun saveSnapshot(snapshot: Snapshot): Boolean {
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
    override fun cancelSnapshotCreation(artifactId: Long, date: String) {
        val snapshotInProgress = metadataRepository.getSnapshotMetadataBlocking(artifactId,
                date)
        SnapshotDownloadCancelRequest().send(context, getDataFolderPath(),
                snapshotInProgress)

        // Updating snapshot status as incomplete in database.
        val incompleteMetadata = snapshotInProgress.copy(status = SnapshotStatus.INCOMPLETE)
        metadataRepository.removeSnapshotMetadata(
                incompleteMetadata.artifactId, incompleteMetadata.date)
        metadataRepository.addSnapshotMetadata(incompleteMetadata)
    }

    /**
     * Removes artifact specified by artifact ID, with all its snapshots metadata.
     * Optionally removes snapshot data as well.
     */
    override fun removeArtifact(artifactId: Long, alsoRemoveData: Boolean) {
        metadataRepository.removeArtifactMetadata(artifactId)
        searchIndexRepository.removeForArtifact(artifactId)
        dataFolderManager.clear(getDataFolderPath(), artifactId)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    override fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean) {
        metadataRepository.removeSnapshotMetadata(artifactId, date)
        searchIndexRepository.removeForSnapshot(artifactId, date)
        dataFolderManager.clear(getDataFolderPath(), artifactId, date)
        // todo alsoRemoveData if needed
    }

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    override fun removeAllSnapshots(alsoRemoveData: Boolean) {
        metadataRepository.clear()
        searchIndexRepository.clear()
        dataFolderManager.clear(getDataFolderPath())
        // todo alsoRemoveData if needed
    }


    private fun deviceStateAllowsDownload(snapshot: Snapshot): Boolean {
        val batteryStateForbidsDownload = !snapshot.downloadSchedule.allowOnLowBattery
                && !DeviceStateUtil.isBatteryChargeMoreThan(context, 20) // todo setting
        if (batteryStateForbidsDownload) {
            Log(context, "Battery charge is too low to download snapshot ${snapshot.title}")
                    .logError()
            return false
        }
        val wifiStateForbidsDownload = snapshot.downloadSchedule.allowOnWifiOnly
                && !DeviceStateUtil.isOnWifi(context)
        if (wifiStateForbidsDownload) {
            Log(context, "WiFi connection is needed to download snapshot ${snapshot.title}")
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
        runningJobManager.putJob(snapshotInProgress)
        postSnapshotDownloadProgress(snapshotInProgress, "Downloading data")

        dataFolderManager.ensureSnapshotFolder(getDataFolderPath(),
                snapshotInProgress.artifactId, snapshotInProgress.date)
        startSnapshotDataTypeDownloader(snapshotInProgress)
    }

    private fun startSnapshotDataTypeDownloader(snapshotInProgress: Snapshot) {
        SnapshotDownloadStartRequest().send(context, getDataFolderPath(),
                snapshotInProgress)
        // Download of snapshot data files will start in a separate service
        // and will finish with the final response SnapshotProgressReceiver invocation.
    }


    // Broadcast receiver for receiving status updates from data type services.
    class SnapshotProgressReceiver : BroadcastReceiver() {
        @Inject
        lateinit var snapshotOperationManager: SnapshotOperationManager
        @Inject
        lateinit var metadataRepository: MetadataRepository

        override fun onReceive(context: Context, intent: Intent) {
            AndroidInjection.inject(this, context)
            val snapshot = metadataRepository.getSnapshotMetadataBlocking(
                    IntentUtil.getArtifactId(intent), IntentUtil.getDate(intent))
            if (IntentUtil.isDownloaded(intent)) {
                GlobalScope.launch(Dispatchers.IO) {
                    snapshotOperationManager.archiveSnapshot(snapshot)
                }
                // Final. Archiving will continue in the main app process.
            } else {
                snapshotOperationManager.postSnapshotDownloadProgress(snapshot, IntentUtil.getMessage(intent))
            }
        }
    }


    /**
     * Packs snapshot data and metadata,
     * then sends archives to folder locations according to metadata.
     */
    override fun archiveSnapshot(snapshotInProgress: Snapshot) {
        if (!runningJobManager.isRunning(snapshotInProgress)) {
            return
        }

        // Packing downloaded snapshot
        val dataFolderPath = dataFolderManager.ensureSnapshotFolder(
                getDataFolderPath(), snapshotInProgress.artifactId,
                snapshotInProgress.date)
        postSnapshotDownloadProgress(snapshotInProgress, "Compressing data")
        val archivePath = ArchiveUtil.packSnapshot(context, dataFolderPath)
        val archiveHashPath = "$archivePath.sha1"
        dataFolderManager.writeTextToFile(HashUtil.getFileHash(archivePath), archiveHashPath)

        // Switching over to complete metadata to archive with data.
        // Note: starting from here existing data will be overwritten
        // even if partially written before.
        val completeSnapshot = snapshotInProgress.copy(status = SnapshotStatus.COMPLETE)

        completeSnapshot.archiveFolderLocations.forEachIndexed { index, dataArchiveLocation -> run {
            if (!runningJobManager.isRunning(completeSnapshot)) {
                return
            }
            postSnapshotDownloadProgress(completeSnapshot,
                    "Saving archive to " + dataArchiveLocation + " "
                            + (index + 1) + " of " + completeSnapshot.archiveFolderLocations.size)
            DestinationUtilResolver.get(dataArchiveLocation.javaClass).writeArchive(
                    context = context,
                    sourceArchivePath = archivePath,
                    sourceHashPath = archiveHashPath,
                    artifactId = completeSnapshot.artifactId,
                    artifactAlias = getArtifactAlias(completeSnapshot.title),
                    date = completeSnapshot.date,
                    archiveFolderLocation = dataArchiveLocation
            )
        } }
        if (!runningJobManager.isRunning(completeSnapshot)) {
            return
        }

        postSnapshotDownloadProgress(completeSnapshot, "Saving metadata to database")
        // finally replacing incomplete metadata with complete one in database
        metadataRepository.removeSnapshotMetadata(
                snapshotInProgress.artifactId, snapshotInProgress.date)
        metadataRepository.addSnapshotMetadata(completeSnapshot)
        dataFolderManager.clearFiles(getDataFolderPath()) // folders remain
        if (!runningJobManager.isRunning(completeSnapshot)) {
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

        if (runningJobManager.isRunning(completeSnapshot)) {
            postSnapshotDownloadProgress(completeSnapshot, "Done")
            postSnapshotDownloadResult(completeSnapshot)
        }
    }

    override fun postSnapshotDownloadProgress(snapshot: Snapshot, message: String) {
        logProgress(message, snapshot)
        GlobalScope.launch(Dispatchers.Main) {
            runningJobManager.invokeJobProgressListener(snapshot, JobProgress(
                    progressMessage = message
            ))
        }
    }


    // utility methods

    private fun getDataFolderPath() = settingsRepository.get().dataFolderPath

    private fun postSnapshotDownloadResult(result: Snapshot) {
        logProgress("Snapshot download complete", result)
        GlobalScope.launch(Dispatchers.Main) {
            runningJobManager.invokeJobProgressListener(result, JobProgress(
                    result = result
            ))
            runningJobManager.removeJob(result)
        }
    }

    private fun logProgress(text: String, snapshot: Snapshot) {
        Log(context, text).snapshot(snapshot).log()
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