/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import android.content.Context
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.MetadataCollection
import com.alexvt.integrity.core.SnapshotMetadata

/**
 * Manager of repository of presets for creating artifact snapshots
 */
interface PresetRepository {
    /**
     * Prepares database for use
     */
    fun init(context: Context)

    /**
     * Adds a unique folder location to choose archive destination from.
     * Adding the existing one does nothing.
     */
    fun addFolderLocation(folderLocation: FolderLocation)

    /**
     * Gets all unique folder locations to choose archive destination from
     */
    fun getAllFolderLocations(): List<FolderLocation>

    /**
     * Deletes all folder locations from database
     */
    fun clear()
}