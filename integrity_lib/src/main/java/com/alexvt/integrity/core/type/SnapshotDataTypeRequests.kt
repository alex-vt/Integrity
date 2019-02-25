/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.util.IntentUtil

/**
 * Requests starting download of snapshot data:
 * first the broadcast is sent to the right package for the data type,
 * then the exact service for the data type is started.
 */
class SnapshotDownloadStartRequest : InterPackageOperation {
    override fun getAction() = "com.alexvt.integrity.SNAPSHOT_DOWNLOAD_START"

    class PackageWideReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            android.util.Log.v("SnapshotDownloadStartReceiver", "onReceive")
            // todo check presence of service before sending broadcast
            DataTypeServiceResolver.startDownloadService(context,
                    IntentUtil.getDataFolderName(intent), IntentUtil.getSnapshot(intent)!!)
        }
    }
}

/**
 * Requests canceling download of snapshot data:
 * first the broadcast is sent to the right package for the data type,
 * then the download job is marked cancelled package-wide.
 */
class SnapshotDownloadCancelRequest : InterPackageOperation {
    override fun getAction() = "com.alexvt.integrity.SNAPSHOT_DOWNLOAD_CANCEL"

    class PackageWideReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val snapshot = IntentUtil.getSnapshot(intent)!!
            RunningJobManager.markJobCanceled(snapshot)
        }
    }
}

/**
 * Operation that may call method in other package, therefore require broadcast.
 */
interface InterPackageOperation {
    fun getAction(): String

    fun send(context: Context, dataFolderName: String, snapshot: Snapshot) {
        BroadcastUtil.sendBroadcast(context, dataFolderName, snapshot, getAction())
    }
}
