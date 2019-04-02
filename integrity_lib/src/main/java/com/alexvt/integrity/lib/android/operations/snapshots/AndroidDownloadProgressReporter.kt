/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.operations.snapshots

import android.content.Context
import android.content.Intent
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.lib.core.operations.snapshots.DownloadProgressReporter

class AndroidDownloadProgressReporter(private val context: Context) : DownloadProgressReporter {

    /**
     * Shows progress, possibly from a different process than the main Integrity app.
     * Therefore, uses BroadcastReceiver.
     */
    override fun reportSnapshotDownloadProgress(artifactId: Long, date: String, message: String) {
        sendSnapshotDownloadProgressBroadcast(context, artifactId, date,
                IntentUtil.withMessage(message))
    }

    /**
     * Shows progress complete, possibly from a different process than the main Integrity app.
     * Therefore, uses BroadcastReceiver.
     */
    override fun reportSnapshotDownloaded(artifactId: Long, date: String) {
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
}