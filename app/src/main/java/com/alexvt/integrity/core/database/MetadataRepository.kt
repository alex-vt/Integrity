/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.database

import android.content.Context
import com.alexvt.integrity.core.MetadataCollection
import com.alexvt.integrity.core.SnapshotMetadata

/**
 * Manager of repository of artifact and snapshot metadata stored in database.
 *
 * Note: Corresponding data in storage is not managed here.
 */
interface MetadataRepository {
    /**
     * Prepares database for use
     */
    fun init(context: Context)

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
    fun addSnapshotMetadata(snapshotMetadata: SnapshotMetadata)

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
    fun getAllArtifactMetadata(): MetadataCollection

    /**
     * Returns list of the latest snapshots
     * of metadata for all artifacts:
     *
     * Complete ones if there are any, otherwise incomplete if there are any, otherwise blueprints.
     */
    fun getAllArtifactLatestMetadata(): MetadataCollection

    /**
     * Returns artifact with all snapshots of metadata by given artifactId.
     */
    fun getArtifactMetadata(artifactId: Long): MetadataCollection

    /**
     * Returns snapshot of metadata by given artifactId and with the most recent date.
     */
    fun getLatestSnapshotMetadata(artifactId: Long): SnapshotMetadata

    /**
     * Returns snapshot of metadata by given artifactId and date.
     */
    fun getSnapshotMetadata(artifactId: Long, date: String): SnapshotMetadata

    /**
     * Removes snapshot metadata marked as blueprints for a given artifact.
     */
    fun cleanupArtifactBlueprints(artifactId: Long)

    /**
     * Deletes all metadata from database
     */
    fun clear()
}