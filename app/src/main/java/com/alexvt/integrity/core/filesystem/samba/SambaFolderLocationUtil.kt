/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem.samba

import android.util.Log
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.filesystem.ArchiveLocationUtil
import com.snatik.storage.Storage
import java.io.File
import jcifs.smb.SmbFile
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking


open class SambaFolderLocationUtil : ArchiveLocationUtil<SambaFolderLocation> {

    override fun getFolderLocationLabel() = "Samba"

    override fun getFolderLocationDescription(folderLocation: SambaFolderLocation)
            = folderLocation.fullPath

    override fun writeArchive(sourceArchivePath: String, sourceHashPath: String,
                              artifactId: Long, artifactAlias: String, date: String,
                              archiveFolderLocation: SambaFolderLocation) {
        Log.d("SambaFolderLocationUtil", "writeArchive")
        runBlocking(Dispatchers.Default) {
            val sambaFolderLocationCredentials = IntegrityCore.folderLocationRepository
                    .getCredentials(archiveFolderLocation) as SambaFolderLocationCredentials
            val sambaAuth = NtlmPasswordAuthentication(
                    null, sambaFolderLocationCredentials.user,
                    sambaFolderLocationCredentials.password)

            val destinationArtifactFolderPath = archiveFolderLocation.fullPath + File.separator +
                    artifactAlias + "_" + artifactId
            createFolders(destinationArtifactFolderPath, sambaAuth)

            val destinationArchivePath = destinationArtifactFolderPath + File.separator +
                    "artifact_" + artifactId + "_snapshot_" + date + ".zip"
            copyFileToSamba(sourceArchivePath, sambaAuth, destinationArchivePath)

            val destinationHashPath = "$destinationArchivePath.sha1"
            copyFileToSamba(sourceHashPath, sambaAuth, destinationHashPath)
        }
    }

    private fun createFolders(folderPath: String, sambaAuth: NtlmPasswordAuthentication) {
        val folder = SmbFile(folderPath, sambaAuth)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    private fun copyFileToSamba(sourcePath: String, sambaAuth: NtlmPasswordAuthentication,
                                sambaDestination: String) {
        val storage = Storage(IntegrityCore.context)
        val sourceFileBytes = storage.readFile(sourcePath)

        val smbFile = SmbFile(sambaDestination, sambaAuth)

        try {
            val out = SmbFileOutputStream(smbFile)
            out.write(sourceFileBytes) // todo buffering
            out.close()
        } catch (e: Exception) {
            Log.e("SambaFolderLocationUtil", "Failed to write Samba file", e)
        }
    }

    override fun readArchive(archiveFolderLocation: SambaFolderLocation, artifactId: Long,
                             date: String, destinationArchivePath: String,
                             destinationHashPath: String) {
        // todo (locate by artifactId and date, then copy to local paths)
    }
}