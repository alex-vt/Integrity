/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations.samba

import android.content.ComponentName
import android.content.Context
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.destinations.DestinationUtil
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocation
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocationCredentials
import com.alexvt.integrity.lib.log.Log
import com.snatik.storage.Storage
import java.io.File
import jcifs.smb.SmbFile
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFileOutputStream


object SambaDestinationUtil : DestinationUtil<SambaFolderLocation> {

    override fun writeArchive(context: Context, sourceArchivePath: String, sourceHashPath: String,
                              artifactId: Long, artifactAlias: String, date: String,
                              archiveFolderLocation: SambaFolderLocation) {
        val sambaFolderLocationCredentials = IntegrityCore.credentialsRepository
                .getCredentials(archiveFolderLocation.title) as SambaFolderLocationCredentials
        val sambaAuth = NtlmPasswordAuthentication(
                null, sambaFolderLocationCredentials.user,
                sambaFolderLocationCredentials.password)

        val destinationArtifactFolderPath = archiveFolderLocation.fullPath + File.separator +
                artifactAlias + "_" + artifactId
        createFolders(destinationArtifactFolderPath, sambaAuth)

        val destinationArchivePath = destinationArtifactFolderPath + File.separator +
                "artifact_" + artifactId + "_snapshot_" + date + ".zip"
        copyFileToSamba(context, sourceArchivePath, sambaAuth, destinationArchivePath)

        val destinationHashPath = "$destinationArchivePath.sha1"
        copyFileToSamba(context, sourceHashPath, sambaAuth, destinationHashPath)
    }

    private fun createFolders(folderPath: String, sambaAuth: NtlmPasswordAuthentication) {
        val folder = SmbFile(folderPath, sambaAuth)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    private fun copyFileToSamba(context: Context, sourcePath: String,
                                sambaAuth: NtlmPasswordAuthentication, sambaDestination: String) {
        val storage = Storage(context)
        val sourceFileBytes = storage.readFile(sourcePath)

        val smbFile = SmbFile(sambaDestination, sambaAuth)

        try {
            val out = SmbFileOutputStream(smbFile)
            out.write(sourceFileBytes) // todo buffering
            out.close()
        } catch (e: Exception) {
            Log(context, "Failed to write Samba file from $sourcePath to $sambaDestination")
                    .logError(e)
        }
    }

    override fun readArchive(archiveFolderLocation: SambaFolderLocation, artifactId: Long,
                             date: String, destinationArchivePath: String,
                             destinationHashPath: String) {
        // todo (locate by artifactId and date, then copy to local paths)
    }

    override fun getViewMainActivityComponent() = ComponentName("com.alexvt.integrity",
            "com.alexvt.integrity.ui.destinations.samba.SambaDestinationActivity")
}