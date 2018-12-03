/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.snatik.storage.Storage
import java.io.File

open class LocalFolderLocationUtil : ArchiveLocationUtil<LocalFolderLocation> {

    override fun getFolderLocationTypeName(): String {
        return "Files on device"
    }

    override fun getFolderLocationDescription(folderLocation: LocalFolderLocation): String {
        return folderLocation.folderPath
    }

    override fun writeArchive(sourceArchivePath: String, sourceHashPath: String,
                              artifactId: Long, artifactAlias: String, date: String,
                              archiveFolderLocation: LocalFolderLocation) {
        Log.d("LocalFolderLocationUtil", "writeArchive")
        val storage = Storage(IntegrityCore.context)
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