/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib

import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.destinations.ArchiveLocationUtil
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.search.DataChunk
import com.alexvt.integrity.core.search.NamedLink
import com.alexvt.integrity.core.util.*
import com.alexvt.integrity.lib.util.DataCacheFolderUtil
import com.alexvt.integrity.lib.util.IntentUtil

/**
 * The entry point of business logic, actions on data and metadata in the Integrity app extension
 */
object IntegrityEx {

    fun isSnapshotDownloadRunning(artifactId: Long, date: String)
            = RunningJobManager.isRunning(artifactId, date)

    fun getSnapshotDataFolderPath(context: Context, dataFolderName: String, artifactId: Long,
                                  date: String): String {
        return DataCacheFolderUtil.getSnapshotFolderPath(context, dataFolderName, artifactId, date)
    }

    fun getSnapshotPreviewPath(context: Context, dataFolderName: String, artifactId: Long,
                               date: String): String {
        return DataCacheFolderUtil.getSnapshotFolderPath(context, dataFolderName, artifactId, date) +
                "/_preview.png"
    }

    fun getSnapshotDataChunksPath(context: Context, dataFolderName: String, artifactId: Long,
                                  date: String): String {
        return DataCacheFolderUtil.getSnapshotFolderPath(context, dataFolderName, artifactId, date) +
                "/_text.txt"
    }

    fun addDataForSearchIndex(context: Context, dataFolderName: String, artifactId: Long,
                              date: String, text: String, index: String,
                              vararg links: Pair<String, String>) {
        val dataChunk = DataChunk(artifactId, date, text, index,
                ArrayList(links.map { NamedLink(it.first, it.second) }))
        val dataChunkJson = JsonSerializerUtil.toJson(dataChunk)

        val textSearchIndexPath = getSnapshotDataChunksPath(context, dataFolderName, artifactId,
                date)

        // todo prevent duplicates
        val prefix = if (DataCacheFolderUtil.fileExists(context, textSearchIndexPath)) ",\n" else ""
        DataCacheFolderUtil.addTextToFile(context, "$prefix$dataChunkJson", textSearchIndexPath)
    }

    /**
     * Shows progress, possibly from a different process than the main Integrity app.
     * Therefore, uses BroadcastReceiver.
     */
    fun reportSnapshotDownloadProgress(context: Context, artifactId: Long, date: String,
                                       message: String) {
        sendSnapshotDownloadProgressBroadcast(context, artifactId, date,
                IntentUtil.withMessage(message))
    }

    /**
     * Shows progress complete, possibly from a different process than the main Integrity app.
     * Therefore, uses BroadcastReceiver.
     */
    fun reportSnapshotDownloaded(context: Context, artifactId: Long, date: String) {
        sendSnapshotDownloadProgressBroadcast(context, artifactId, date,
                IntentUtil.withDownloaded(true))
    }

    private fun sendSnapshotDownloadProgressBroadcast(context: Context, artifactId: Long,
                                                      date: String, progressOnlyIntent: Intent) {
        context.applicationContext.sendBroadcast(progressOnlyIntent.apply {
            action = "com.alexvt.integrity.SNAPSHOT_DOWNLOAD_PROGRESS"
            IntentUtil.putArtifactId(this, artifactId)
            IntentUtil.putDate(this, date)
        })
    }

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> getFileLocationUtil(dataArchiveLocationClass: Class<F>): ArchiveLocationUtil<F> {
        val utilClassName = dataArchiveLocationClass.name
                .replace("FolderLocation", "LocationUtil")
        return Class.forName(utilClassName).kotlin.objectInstance as ArchiveLocationUtil<F>
    }

    fun getFolderLocationNames(archiveFolderLocations: List<FolderLocation>) = archiveFolderLocations
            .map { getFolderLocationName(it) }
            .toTypedArray()

    fun getFolderLocationName(folderLocation: FolderLocation) = folderLocation.title +
            " (" + getFileLocationUtil(folderLocation.javaClass).getFolderLocationLabel() + "): " +
            getFileLocationUtil(folderLocation.javaClass).getFolderLocationDescription(folderLocation)



    /**
     * TypeSpecificConverters
     */

    fun toTypeSpecificMetadata(snapshot: Snapshot) = SnapshotMetadata(
            artifactId = snapshot.artifactId,
            title = snapshot.title,
            date = snapshot.date,
            description = snapshot.description,
            downloadSchedule = snapshot.downloadSchedule,
            archiveFolderLocations = snapshot.archiveFolderLocations,
            tags = snapshot.tags,
            themeColor = snapshot.themeColor,
            dataTypeSpecificMetadata = JsonSerializerUtil.fromJson(
                    snapshot.dataTypeSpecificMetadataJson,
                    Class.forName(snapshot.dataTypeClassName) as Class<TypeMetadata>),
            status = snapshot.status
    )

    fun fromTypeSpecificMetadata(context: Context, snapshotMetadata: SnapshotMetadata) = Snapshot(
            artifactId = snapshotMetadata.artifactId,
            title = snapshotMetadata.title,
            date = snapshotMetadata.date,
            description = snapshotMetadata.description,
            downloadSchedule = snapshotMetadata.downloadSchedule,
            archiveFolderLocations = snapshotMetadata.archiveFolderLocations,
            tags = snapshotMetadata.tags,
            themeColor = snapshotMetadata.themeColor,
            dataTypeClassName = snapshotMetadata.dataTypeSpecificMetadata.javaClass.name,
            dataTypePackageName = context.packageName,
            dataTypeSpecificMetadataJson = JsonSerializerUtil.toJson(
                    snapshotMetadata.dataTypeSpecificMetadata),
            status = snapshotMetadata.status
    )
}