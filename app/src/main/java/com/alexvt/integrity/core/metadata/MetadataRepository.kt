/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.metadata

import com.alexvt.integrity.core.util.Clearable
import com.alexvt.integrity.lib.metadata.Snapshot
import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Manager of repository of artifact and snapshot metadata stored in database.
 *
 * Note: Corresponding data in storage is not managed here.
 */
interface MetadataRepository : Clearable {

    /**
     * Writes snapshot metadata to database.
     *
     * If snapshots with the same artifactId don't exist,
     * the snapshot will be the first one of this artifact.
     *
     * If snapshot with the same artifactId and date exists,
     * its metadata in database will be updated.
     *
     * Corresponding data in storage is not managed here.
     * Please make sure to write or update snapshot with correct data path.
     */
    fun addSnapshotMetadata(snapshot: Snapshot)

    /**
     * Removes all snapshots metadata of artifact specified by artifactId.
     *
     * Corresponding data in storage is not affected.
     */
    fun removeArtifactMetadata(artifactId: Long)

    /**
     * Removes snapshot metadata of snapshot specified by artifactId and date.
     *
     * Corresponding data in storage is not affected.
     */
    fun removeSnapshotMetadata(artifactId: Long, date: String)

    /**
     * Returns list of all snapshots of metadata stored in database, for all artifacts.
     */
    fun getAllArtifactMetadataBlocking(): List<Snapshot>

    fun getAllArtifactMetadataFlowable(): Flowable<List<Snapshot>>

    /**
     * Returns list of the latest snapshots
     * of metadata for all artifacts.
     *
     * If deprioritizeBlueprints is true and there are non-blueprint snapshots,
     * the latest of them will be used.
     */
    fun getAllArtifactLatestMetadataBlocking(deprioritizeBlueprints: Boolean): List<Snapshot>

    fun getAllArtifactLatestMetadataFlowable(deprioritizeBlueprints: Boolean): Flowable<List<Snapshot>>

    /**
     * Returns artifact with all snapshots of metadata by given artifactId.
     */
    fun getArtifactMetadataBlocking(artifactId: Long): List<Snapshot>

    fun getArtifactMetadataFlowable(artifactId: Long): Flowable<List<Snapshot>>

    /**
     * Returns snapshot of metadata by given artifactId and with the most recent date.
     */
    fun getLatestSnapshotMetadataBlocking(artifactId: Long): Snapshot

    fun getLatestSnapshotMetadataFlowable(artifactId: Long): Flowable<Snapshot>

    /**
     * Returns snapshot of metadata by given artifactId and date.
     */
    fun getSnapshotMetadataBlocking(artifactId: Long, date: String): Snapshot

    fun getSnapshotMetadataFlowable(artifactId: Long, date: String): Flowable<Snapshot>

    /**
     * Removes snapshot metadata marked as blueprints for a given artifact.
     */
    fun cleanupArtifactBlueprints(artifactId: Long)

    /**
     * Deletes all metadata from database
     */
    override fun clear()
}