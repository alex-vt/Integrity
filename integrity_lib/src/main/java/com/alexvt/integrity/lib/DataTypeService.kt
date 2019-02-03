/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.JobIntentService
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.core.util.JsonSerializerUtil

/**
 * Data manipulation contract for a data type, based on Android services (and activities).
 *
 * Reliance on Android services and activities allows loose modularity:
 * Integrity app extensions as separate apps.
 *
 * Generic parameter defines the type dependent metadata model type which this service works with
 */
abstract class DataTypeService<T: TypeMetadata>: JobIntentService() {

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
    abstract fun downloadData(artifactId: Long, date: String, typeMetadata: T): String

    /**
     * Generates snapshot preview image using downloaded snapshot data.
     */
    abstract fun generateOfflinePreview(artifactId: Long, date: String, typeMetadata: T): Bitmap

    /**
     * Starts service job execution according to intent extras.
     */
    final override fun onHandleWork(intent: Intent) {
        createSnapshotFiles(IntentUtil.getSnapshot(intent)!!)
    }

    /**
     * Downloads snapshot data and creates corresponding files and a metadata file.
     */
    private fun createSnapshotFiles(snapshot: Snapshot) {
        IntegrityEx.reportSnapshotDownloadProgress(applicationContext, snapshot.artifactId,
                snapshot.date, "Saving snapshot")
        RunningJobManager.putJob(snapshot)

        val dataFolderPath = downloadData(snapshot.artifactId, snapshot.date,
                getTypeMetadata(snapshot))
        writeMetadataFile(dataFolderPath, snapshot)

        IntegrityEx.reportSnapshotDownloadProgress(applicationContext, snapshot.artifactId,
                snapshot.date, "Saving preview")
        val previewScreenshot = generateOfflinePreview(snapshot.artifactId, snapshot.date,
                getTypeMetadata(snapshot))
        writePreviewScreenshotFile(dataFolderPath, previewScreenshot)

        // RunningJobManager job shouldn't be removed because job may continue in same process.
        IntegrityEx.reportSnapshotDownloaded(applicationContext, snapshot.artifactId, snapshot.date)
    }


    // utility methods

    private fun getTypeMetadata(snapshot: Snapshot) = JsonSerializerUtil.fromJson(
            snapshot.dataTypeSpecificMetadataJson, getTypeMetadataClass())

    private fun writeMetadataFile(dataFolderPath: String, snapshot: Snapshot) {
        val metadataFilePath = "$dataFolderPath/_metadata.json.txt"
        val snapshotMetadata = IntegrityCore.toTypeSpecificMetadata(snapshot)
        DataCacheFolderUtil.writeTextToFile(applicationContext,
                JsonSerializerUtil.toJson(snapshotMetadata), metadataFilePath)
    }

    private fun writePreviewScreenshotFile(dataFolderPath: String, previewScreenshot: Bitmap) {
        val screenshotFilePath = "$dataFolderPath/_preview.png"
        DataCacheFolderUtil.writeImageToFile(applicationContext,
                previewScreenshot, screenshotFilePath)
    }
}