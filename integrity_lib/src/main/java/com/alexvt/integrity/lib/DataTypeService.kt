/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib

import android.app.Activity
import android.content.Intent
import androidx.core.app.JobIntentService
import com.alexvt.integrity.lib.filesystem.AndroidFilesystemManager
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.TypeMetadata
import com.alexvt.integrity.lib.operations.SnapshotDownloadReporter
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.lib.util.JsonSerializerUtil
import com.alexvt.integrity.lib.util.TypeSpecificMetadataConverter

/**
 * Data manipulation contract for a data type, based on Android services (and activities).
 *
 * Reliance on Android services and activities allows loose modularity:
 * Integrity app extensions as separate apps.
 *
 * Generic parameter defines the type dependent metadata model type which this service works with
 */
abstract class DataTypeService<T: TypeMetadata>: JobIntentService() {

    protected val dataFolderManager: DataFolderManager by lazy {
        DataFolderManager(AndroidFilesystemManager(this))
    }

    /**
     * Gets data type name visible for user
     */
    abstract fun getTypeScreenName(): String

    /**
     * Gets generic parameter class of a derived class.
     * A workaround needed because of JVM generic type erasure. // todo look for elegant solution
     */
    abstract fun getTypeMetadataClass(): Class<T>

    /**
     * Gets class of activity responsible for (starting) operations with the data type.
     *
     * Activity should accept in intent:
     * artifactId and date for viewing snapshot,
     * only artifactId for creating new snapshot for the existing artifact,
     * or neither for creating snapshot of new artifact.
     */
    abstract fun getViewingActivityClass(): Class<out Activity> // todo put T to base activity

    /**
     * Downloads type specific data described by given data type specific metadata.
     */
    abstract fun downloadData(dataFolderName: String, artifactId: Long, date: String,
                              typeMetadata: T): String

    /**
     * Generates snapshot preview image using downloaded snapshot data.
     */
    abstract fun generateOfflinePreview(dataFolderName: String, artifactId: Long, date: String,
                                        typeMetadata: T)

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
        SnapshotDownloadReporter.reportSnapshotDownloadProgress(applicationContext, snapshot.artifactId,
                snapshot.date, "Saving snapshot")
        IntegrityLib.runningJobManager.putJob(snapshot)

        val dataFolderPath = downloadData(dataFolderName, snapshot.artifactId, snapshot.date,
                getTypeMetadata(snapshot))
        writeMetadataFile(dataFolderPath, snapshot)

        if (!IntegrityLib.runningJobManager.isRunning(snapshot.artifactId, snapshot.date)) {
            return
        }
        SnapshotDownloadReporter.reportSnapshotDownloadProgress(applicationContext, snapshot.artifactId,
                snapshot.date, "Saving preview")
        generateOfflinePreview(dataFolderName, snapshot.artifactId, snapshot.date,
                getTypeMetadata(snapshot))

        // RunningJobManager job shouldn't be removed because job may continue in same process.
        SnapshotDownloadReporter.reportSnapshotDownloaded(applicationContext, snapshot.artifactId, snapshot.date)
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