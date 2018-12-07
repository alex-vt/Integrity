/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import android.content.Context
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.FolderLocationCredentials
import com.alexvt.integrity.core.MetadataCollection
import com.alexvt.integrity.core.SnapshotMetadata

/**
 * Manager of repository of folder locations for saving artifact snapshots to
 */
interface FolderLocationRepository {
    /**
     * Prepares database for use
     */
    fun init(context: Context)

    /**
     * Adds a unique folder location to choose archive destination from.
     * Returns folder location ID
     */
    fun addFolderLocation(folderLocation: FolderLocation): String

    /**
     * Adds folder location credentials which are stored separately.
     */
    fun addFolderLocationCredentials(folderLocationCredentials: FolderLocationCredentials)

    /**
     * Gets credentials for the provided folder location. If none, returns empty ones.
     */
    fun getCredentials(folderLocation: FolderLocation): FolderLocationCredentials

    /**
     * Gets all unique folder locations to choose archive destination from
     */
    fun getAllFolderLocations(): List<FolderLocation>

    fun removeFolderLocationAndCredentials(title: String)

    /**
     * Deletes all folder locations and credentials from database
     */
    fun clear()
}