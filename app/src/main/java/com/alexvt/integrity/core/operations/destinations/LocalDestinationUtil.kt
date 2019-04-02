/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.destinations

import com.alexvt.integrity.lib.core.data.destinations.LocalFolderLocation
import com.alexvt.integrity.lib.core.operations.filesystem.FilesystemManager
import java.io.File
import javax.inject.Inject

class LocalDestinationUtil @Inject constructor(
        private val filesystemManager: FilesystemManager
) : DestinationUtil<LocalFolderLocation> {

    override fun writeArchive(sourceArchivePath: String, sourceHashPath: String,
                              artifactId: Long, artifactAlias: String, date: String,
                              archiveFolderLocation: LocalFolderLocation) {
        filesystemManager.createFolder(archiveFolderLocation.path)
        val destinationArtifactFolderPath = filesystemManager.getRootFolder() + File.separator +
                archiveFolderLocation.path + File.separator + artifactAlias + "_" + artifactId
        filesystemManager.createFolder(destinationArtifactFolderPath)

        val destinationArchivePath = destinationArtifactFolderPath + File.separator +
                "artifact_" + artifactId + "_snapshot_" + date + ".zip"
        filesystemManager.copyFolder(sourceArchivePath, destinationArchivePath)

        val destinationHashPath = "$destinationArchivePath.sha1"
        filesystemManager.copyFolder(sourceHashPath, destinationHashPath)
    }

    override fun readArchive(archiveFolderLocation: LocalFolderLocation, artifactId: Long,
                             date: String, destinationArchivePath: String,
                             destinationHashPath: String) {
        // todo (locate by artifactId and date, then copy to local paths)
    }

    override fun getViewMainActivityComponent() = Pair("com.alexvt.integrity",
            "com.alexvt.integrity.android.ui.destinations.local.LocalDestinationActivity")
}