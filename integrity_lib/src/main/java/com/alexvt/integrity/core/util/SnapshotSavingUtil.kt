/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import com.alexvt.integrity.core.job.JobProgress
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.job.ScheduledJobManager
import com.alexvt.integrity.core.search.DataChunks
import com.alexvt.integrity.core.type.SnapshotDownloadStartRequest
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.Log
import com.alexvt.integrity.lib.util.DataCacheFolderUtil
import com.alexvt.integrity.lib.util.IntentUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

/**
 * Saves snapshot metadata and data.
 */
object SnapshotSavingUtil {

    /**
     * Saves snapshot data and/or metadata blueprint according to its status.
     */
    fun saveSnapshot(context: Context, snapshot: Snapshot) {
        if (snapshot.status == SnapshotStatus.BLUEPRINT
                || snapshot.status == SnapshotStatus.IN_PROGRESS) {
            saveSnapshotBlueprint(context, snapshot)
        }
        if (snapshot.status == SnapshotStatus.IN_PROGRESS
                || snapshot.status == SnapshotStatus.INCOMPLETE) {
            val snapshotBlueprint = IntegrityCore.metadataRepository
                    .getLatestSnapshotMetadata(snapshot.artifactId)
            if (deviceStateAllowsDownload(context, snapshot)) {
                downloadFromBlueprint(context, snapshotBlueprint)
                if (context is Activity) {
                    IntegrityCore.showRunningJobProgressDialog(context,
                            snapshotBlueprint.artifactId, snapshotBlueprint.date)
                }
            }
        }
    }

    private fun deviceStateAllowsDownload(context: Context, snapshot: Snapshot): Boolean {
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
    private fun saveSnapshotBlueprint(context: Context, snapshot: Snapshot): String {
        val date = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(System.currentTimeMillis())
        IntegrityCore.metadataRepository.cleanupArtifactBlueprints(context, snapshot.artifactId) // no old ones
        IntegrityCore.metadataRepository.addSnapshotMetadata(context, snapshot.copy(
                date = date,
                status = SnapshotStatus.BLUEPRINT
        ))
        ScheduledJobManager.updateSchedule(context)
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
    private fun downloadFromBlueprint(context: Context, blueprintSnapshot: Snapshot) {
        // overwriting the previous snapshot state in database with In Progress
        val snapshotInProgress = blueprintSnapshot.copy(status = SnapshotStatus.IN_PROGRESS)
        IntegrityCore.metadataRepository.removeSnapshotMetadata(context,
                snapshotInProgress.artifactId, snapshotInProgress.date)
        IntegrityCore.metadataRepository.addSnapshotMetadata(context, snapshotInProgress)

        // starting download
        RunningJobManager.putJob(snapshotInProgress)
        postSnapshotDownloadProgress(context, snapshotInProgress, "Downloading data")

        DataCacheFolderUtil.ensureSnapshotFolder(context, IntegrityCore.getDataFolderName(),
                snapshotInProgress.artifactId, snapshotInProgress.date)
        SnapshotDownloadStartRequest().send(context, IntegrityCore.getDataFolderName(),
                snapshotInProgress)
        // Download of snapshot data files will start in a separate service
        // and will finish with the final response SnapshotProgressReceiver invocation.
    }

    // Broadcast receiver for receiving status updates from the IntentService.
    class SnapshotProgressReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(
                    IntentUtil.getArtifactId(intent), IntentUtil.getDate(intent))
            if (IntentUtil.isDownloaded(intent)) {
                GlobalScope.launch(Dispatchers.IO) {
                    archiveSnapshot(context, snapshot)
                }
                // Final. Archiving will continue in the main app process.
            } else {
                postSnapshotDownloadProgress(context, snapshot, IntentUtil.getMessage(intent))
            }
        }
    }

    /**
     * Packs snapshot data and metadata,
     * then sends archives to folder locations according to metadata.
     */
    private fun archiveSnapshot(context: Context, snapshotInProgress: Snapshot) {
        if (!RunningJobManager.isRunning(snapshotInProgress)) {
            return
        }

        // Packing downloaded snapshot
        val dataFolderName = IntegrityCore.getDataFolderName()
        val dataFolderPath = DataCacheFolderUtil.ensureSnapshotFolder(context,
                dataFolderName, snapshotInProgress.artifactId,
                snapshotInProgress.date)
        postSnapshotDownloadProgress(context, snapshotInProgress, "Compressing data")
        val archivePath = ArchiveUtil.packSnapshot(dataFolderPath)
        val archiveHashPath = "$archivePath.sha1"
        DataCacheFolderUtil.writeTextToFile(context, HashUtil.getFileHash(archivePath), archiveHashPath)

        // Switching over to complete metadata to archive with data.
        // Note: starting from here existing data will be overwritten
        // even if partially written before.
        val completeSnapshot = snapshotInProgress.copy(status = SnapshotStatus.COMPLETE)

        completeSnapshot.archiveFolderLocations.forEachIndexed { index, dataArchiveLocation -> run {
            if (!RunningJobManager.isRunning(completeSnapshot)) {
                return
            }
            postSnapshotDownloadProgress(context, completeSnapshot,
                    "Saving archive to " + dataArchiveLocation + " "
                    + (index + 1) + " of " + completeSnapshot.archiveFolderLocations.size)
            IntegrityCore.getFileLocationUtil(dataArchiveLocation.javaClass).writeArchive(
                    context = context,
                    sourceArchivePath = archivePath,
                    sourceHashPath = archiveHashPath,
                    artifactId = completeSnapshot.artifactId,
                    artifactAlias = getArtifactAlias(completeSnapshot.title),
                    date = completeSnapshot.date,
                    archiveFolderLocation = dataArchiveLocation
            )
        } }
        if (!RunningJobManager.isRunning(completeSnapshot)) {
            return
        }

        postSnapshotDownloadProgress(context, completeSnapshot, "Saving metadata to database")
        // finally replacing incomplete metadata with complete one in database
        IntegrityCore.metadataRepository.removeSnapshotMetadata(context,
                snapshotInProgress.artifactId, snapshotInProgress.date)
        IntegrityCore.metadataRepository.addSnapshotMetadata(context, completeSnapshot)
        DataCacheFolderUtil.clearFiles(context, dataFolderName) // folders remain
        if (!RunningJobManager.isRunning(completeSnapshot)) {
            return
        }

        if (DataCacheFolderUtil.fileExists(context, IntegrityEx.getSnapshotDataChunksPath(context,
                        dataFolderName, completeSnapshot.artifactId, completeSnapshot.date))) {
            postSnapshotDownloadProgress(context, completeSnapshot, "Updating search index")
            val dataChunkListJson = DataCacheFolderUtil.getTextFromFile(context,
                    IntegrityEx.getSnapshotDataChunksPath(context, dataFolderName,
                    completeSnapshot.artifactId, completeSnapshot.date))
            val dataChunks = JsonSerializerUtil.fromJson("{\"chunks\": [$dataChunkListJson]}",
                    DataChunks::class.java).chunks // todo make clean, ensure no duplicates
            IntegrityCore.searchIndexRepository.add(context, dataChunks)
        }

        if (RunningJobManager.isRunning(completeSnapshot)) {
            postSnapshotDownloadProgress(context, completeSnapshot, "Done")
            postSnapshotDownloadResult(context, completeSnapshot)
        }
        ScheduledJobManager.updateSchedule(context)
    }


    // utility methods

    private fun postSnapshotDownloadProgress(context: Context, snapshot: Snapshot, message: String) {
        Log(context, message).snapshot(snapshot).log()
        GlobalScope.launch(Dispatchers.Main) {
            RunningJobManager.invokeJobProgressListener(snapshot, JobProgress(
                    progressMessage = message
            ))
        }
    }

    private fun postSnapshotDownloadResult(context: Context, result: Snapshot) {
        Log(context, "Snapshot download complete").snapshot(result).log()
        GlobalScope.launch(Dispatchers.Main) {
            RunningJobManager.invokeJobProgressListener(result, JobProgress(
                    result = result
            ))
            RunningJobManager.removeJob(result)
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