/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations

import android.content.ComponentName
import android.content.Context
import com.alexvt.integrity.lib.FolderLocation

/**
 * Data archive read/write contract for a type of viewable folder location
 */
interface DestinationUtil<F: FolderLocation> {

    /**
     * Writes file from sourceFilePath
     * in a folder and optionally a subfolder
     * defined by dataArchiveLocation, artifactAlias and artifactId.
     */
    fun writeArchive(context: Context, sourceArchivePath: String, sourceHashPath: String,
                     artifactId: Long, artifactAlias: String, date: String,
                     archiveFolderLocation: F)

    fun readArchive(archiveFolderLocation: F, artifactId: Long, date: String,
                    destinationArchivePath: String, destinationHashPath: String)

    /**
     * Gets component name of activity responsible for (starting) viewing or editing
     * this folder location.
     *
     * Activity should accept in intent:
     * title of file location to view/edit.
     */
    fun getViewMainActivityComponent(): ComponentName
}