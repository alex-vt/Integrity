/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.snapshots

interface DownloadProgressReporter {

    /**
     * Shows progress, possibly from a different process than the main Integrity app.
     */
    fun reportSnapshotDownloadProgress(artifactId: Long, date: String, message: String)

    /**
     * Shows progress complete, possibly from a different process than the main Integrity app.
     */
    fun reportSnapshotDownloaded(artifactId: Long, date: String)

}