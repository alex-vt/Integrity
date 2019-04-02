/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.operations.snapshots

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.data.device.DeviceInfoRepository
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.data.search.SearchIndexRepository
import com.alexvt.integrity.core.operations.snapshots.ArchiveManager
import com.alexvt.integrity.core.operations.snapshots.SnapshotOperationManager
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.operations.destinations.DestinationUtilManager
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.data.jobs.RunningJobRepository
import com.alexvt.integrity.lib.android.operations.snapshots.SnapshotDownloadCancelRequest
import com.alexvt.integrity.lib.android.operations.snapshots.SnapshotDownloadStartRequest
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.lib.core.operations.log.LogManager
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * Saves snapshot metadata and data.
 */
class AndroidSnapshotOperationManager @Inject constructor(
        private val context: Context,
        metadataRepository: MetadataRepository,
        searchIndexRepository: SearchIndexRepository,
        dataFolderManager: DataFolderManager,
        runningJobRepository: RunningJobRepository,
        settingsRepository: SettingsRepository,
        archiveManager: ArchiveManager,
        deviceInfoRepository: DeviceInfoRepository,
        logManager: LogManager,
        destinationUtilManager: DestinationUtilManager
) : SnapshotOperationManager(metadataRepository, searchIndexRepository, dataFolderManager,
        runningJobRepository, settingsRepository, archiveManager, deviceInfoRepository, logManager,
        destinationUtilManager) {

    override fun terminateDownload(snapshot: Snapshot) {
        SnapshotDownloadCancelRequest().send(context, getDataFolderPath(), snapshot)
    }

    override fun startSnapshotDataTypeDownloader(snapshotInProgress: Snapshot) {
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
            SnapshotOperationManager.continueSavingSnapshot(
                    snapshotOperationManager,
                    metadataRepository,
                    IntentUtil.getArtifactId(intent),
                    IntentUtil.getDate(intent),
                    IntentUtil.isDownloaded(intent),
                    IntentUtil.getMessage(intent)
            )
        }
    }
}