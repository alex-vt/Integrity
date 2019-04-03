/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.destinations

import com.alexvt.integrity.core.data.credentials.CredentialsRepository
import com.alexvt.integrity.lib.core.data.destinations.SambaFolderLocation
import com.alexvt.integrity.lib.core.data.destinations.SambaFolderLocationCredentials
import com.alexvt.integrity.lib.core.data.filesystem.FileRepository
import com.alexvt.integrity.lib.core.operations.log.Logger
import java.io.File
import jcifs.smb.SmbFile
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFileOutputStream
import javax.inject.Inject


class SambaDestinationUtil @Inject constructor(
        private val credentialsRepository: CredentialsRepository,
        private val fileRepository: FileRepository,
        private val logger: Logger
) : DestinationUtil<SambaFolderLocation> {

    override fun writeArchive(sourceArchivePath: String, sourceHashPath: String,
                              artifactId: Long, artifactAlias: String, date: String,
                              archiveFolderLocation: SambaFolderLocation) {
        val sambaFolderLocationCredentials = credentialsRepository
                .getCredentialsBlocking(archiveFolderLocation.title) as SambaFolderLocationCredentials
        val sambaAuth = NtlmPasswordAuthentication(
                null, sambaFolderLocationCredentials.user,
                sambaFolderLocationCredentials.password)

        val destinationArtifactFolderPath = archiveFolderLocation.path + File.separator +
                artifactAlias + "_" + artifactId
        createFolders(destinationArtifactFolderPath, sambaAuth)

        val destinationArchivePath = destinationArtifactFolderPath + File.separator +
                "artifact_" + artifactId + "_snapshot_" + date + ".zip"
        copyFileToSamba(sourceArchivePath, sambaAuth, destinationArchivePath)

        val destinationHashPath = "$destinationArchivePath.sha1"
        copyFileToSamba(sourceHashPath, sambaAuth, destinationHashPath)
    }

    private fun createFolders(folderPath: String, sambaAuth: NtlmPasswordAuthentication) {
        val folder = SmbFile(folderPath, sambaAuth)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    private fun copyFileToSamba(sourcePath: String, sambaAuth: NtlmPasswordAuthentication,
                                sambaDestination: String) {
        val sourceFileBytes = fileRepository.getBytes(sourcePath)

        val smbFile = SmbFile(sambaDestination, sambaAuth)

        try {
            val out = SmbFileOutputStream(smbFile)
            out.write(sourceFileBytes) // todo buffering
            out.close()
        } catch (e: Exception) {
            logger.logError("Failed to write Samba file from $sourcePath to $sambaDestination")
        }
    }

    override fun readArchive(archiveFolderLocation: SambaFolderLocation, artifactId: Long,
                             date: String, destinationArchivePath: String,
                             destinationHashPath: String) {
        // todo (locate by artifactId and date, then copy to local paths)
    }

    override fun getViewMainActivityComponent() = Pair("com.alexvt.integrity",
            "com.alexvt.integrity.android.ui.destinations.samba.SambaDestinationActivity")
}