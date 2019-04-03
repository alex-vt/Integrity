/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.operations.snapshots

import android.content.Intent
import androidx.core.app.JobIntentService
import com.alexvt.integrity.lib.android.data.filesystem.AndroidFileRepository
import com.alexvt.integrity.lib.android.operations.log.AndroidLogger
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.metadata.TypeMetadata
import com.alexvt.integrity.lib.core.data.jobs.GlobalRunningJobs
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.lib.core.operations.log.Logger
import com.alexvt.integrity.lib.core.operations.snapshots.DataTypeDownloader
import com.alexvt.integrity.lib.core.operations.snapshots.DownloadProgressReporter
import com.alexvt.integrity.lib.core.util.JsonSerializerUtil
import com.alexvt.integrity.lib.android.util.TypeSpecificMetadataConverter

/**
 * Data manipulation contract for a data type, based on Android services (and activities).
 *
 * Reliance on Android services and activities allows loose modularity:
 * Integrity app extensions as separate apps.
 *
 * Generic parameter defines the type dependent metadata model type which this service works with
 */
abstract class DataTypeService<T: TypeMetadata>: JobIntentService(), DataTypeDownloader<T> {

    protected val dataFolderManager: DataFolderManager by lazy {
        DataFolderManager(AndroidFileRepository(this))
    }
    private val downloadProgressReporter: DownloadProgressReporter by lazy {
        AndroidDownloadProgressReporter(this)
    }
    protected val logger: Logger by lazy {
        AndroidLogger(this)
    }

    /**
     * Starts service job execution according to intent extras.
     */
    final override fun onHandleWork(intent: Intent) {
        createSnapshotFiles(IntentUtil.getDataFolderName(intent), IntentUtil.getSnapshot(intent)!!)
    }

    /**
     * Downloads snapshot data and creates corresponding files and a metadata file.
     */
    private fun createSnapshotFiles(dataFolderName: String, snapshot: Snapshot) {
        downloadProgressReporter.reportSnapshotDownloadProgress(snapshot.artifactId,
                snapshot.date, "Saving snapshot")
        GlobalRunningJobs.RUNNING_JOB_REPOSITORY.putJob(snapshot)

        val dataFolderPath = downloadData(dataFolderName, snapshot.artifactId, snapshot.date,
                getTypeMetadata(snapshot))
        writeMetadataFile(dataFolderPath, snapshot)

        if (!GlobalRunningJobs.RUNNING_JOB_REPOSITORY.isRunning(snapshot.artifactId, snapshot.date)) {
            return
        }
        downloadProgressReporter.reportSnapshotDownloadProgress(snapshot.artifactId,
                snapshot.date, "Saving preview")
        generateOfflinePreview(dataFolderName, snapshot.artifactId, snapshot.date,
                getTypeMetadata(snapshot))

        // RunningJobManager job shouldn't be removed because job may continue in same process.
        downloadProgressReporter.reportSnapshotDownloaded(snapshot.artifactId, snapshot.date)
    }


    // utility methods

    private fun getTypeMetadata(snapshot: Snapshot) = JsonSerializerUtil.fromJson(
            snapshot.dataTypeSpecificMetadataJson, getTypeMetadataClass())

    private fun writeMetadataFile(dataFolderPath: String, snapshot: Snapshot) {
        val metadataFilePath = "$dataFolderPath/_metadata.json.txt"
        val snapshotMetadata = TypeSpecificMetadataConverter.toTypeSpecificMetadata(snapshot)
        dataFolderManager.writeTextToFile(
                JsonSerializerUtil.toJson(snapshotMetadata), metadataFilePath)
    }
}