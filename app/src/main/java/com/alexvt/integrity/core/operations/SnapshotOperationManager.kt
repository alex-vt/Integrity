/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations

import com.alexvt.integrity.lib.metadata.Snapshot

/**
 * Saves, interrupts saving or removes snapshot metadata and data.
 * todo more strict interface
 */
interface SnapshotOperationManager {

    /**
     * Saves snapshot data and/or metadata blueprint according to its status.
     * @return true when snapshot is saving
     */
    fun saveSnapshot(snapshot: Snapshot): Boolean

    /**
     * Cancels long running job if it's running. Metadata status changes to Incomplete.
     */
    fun cancelSnapshotCreation(artifactId: Long, date: String)

    /**
     * Removes artifact specified by artifact ID, with all its snapshots metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeArtifact(artifactId: Long, alsoRemoveData: Boolean)

    /**
     * Removes snapshot metadata specified by artifact ID and date.
     * Optionally removes snapshot data as well.
     */
    fun removeSnapshot(artifactId: Long, date: String, alsoRemoveData: Boolean)

    /**
     * Removes all snapshot metadata.
     * Optionally removes snapshot data as well.
     */
    fun removeAllSnapshots(alsoRemoveData: Boolean)

    /**
     * Packs snapshot data and metadata,
     * then sends archives to folder locations according to metadata.
     */
    fun archiveSnapshot(snapshotInProgress: Snapshot)

    fun postSnapshotDownloadProgress(snapshot: Snapshot, message: String)
}