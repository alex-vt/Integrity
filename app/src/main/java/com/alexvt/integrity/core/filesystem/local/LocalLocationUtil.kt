/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem.local

import android.content.Context
import com.alexvt.integrity.core.filesystem.ArchiveLocationUtil
import com.snatik.storage.Storage
import java.io.File

open class LocalLocationUtil : ArchiveLocationUtil<LocalFolderLocation> {

    override fun getFolderLocationLabel() = "Local"

    override fun getFolderLocationDescription(folderLocation: LocalFolderLocation)
            = folderLocation.folderPath

    override fun getViewMainActivityClass() = LocalLocationActivity::class.java

    override fun writeArchive(context: Context, sourceArchivePath: String, sourceHashPath: String,
                              artifactId: Long, artifactAlias: String, date: String,
                              archiveFolderLocation: LocalFolderLocation) {
        val storage = Storage(context)
        storage.createDirectory(archiveFolderLocation.folderPath)
        val destinationArtifactFolderPath = archiveFolderLocation.folderPath + File.separator +
                artifactAlias + "_" + artifactId
        storage.createDirectory(destinationArtifactFolderPath)

        val destinationArchivePath = destinationArtifactFolderPath + File.separator +
                "artifact_" + artifactId + "_snapshot_" + date + ".zip"
        storage.copy(sourceArchivePath, destinationArchivePath)

        val destinationHashPath = "$destinationArchivePath.sha1"
        storage.copy(sourceHashPath, destinationHashPath)
    }

    override fun readArchive(archiveFolderLocation: LocalFolderLocation, artifactId: Long,
                             date: String, destinationArchivePath: String,
                             destinationHashPath: String) {
        // todo (locate by artifactId and date, then copy to local paths)
    }
}