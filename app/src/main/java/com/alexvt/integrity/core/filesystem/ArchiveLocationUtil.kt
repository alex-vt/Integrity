/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import com.alexvt.integrity.core.FolderLocation

/**
 * Data archive read/write contract for a type of folder location
 */
interface ArchiveLocationUtil<F: FolderLocation> {

    fun getFolderLocationLabel(): String

    fun getFolderLocationDescription(folderLocation: F): String

    /**
     * Writes file from sourceFilePath
     * in a folder and optionally a subfolder
     * defined by dataArchiveLocation, artifactAlias and artifactId.
     */
    fun writeArchive(sourceArchivePath: String, sourceHashPath: String,
                     artifactId: Long, artifactAlias: String, date: String,
                     archiveFolderLocation: F)

    fun readArchive(archiveFolderLocation: F, artifactId: Long, date: String,
                    destinationArchivePath: String, destinationHashPath: String)
}