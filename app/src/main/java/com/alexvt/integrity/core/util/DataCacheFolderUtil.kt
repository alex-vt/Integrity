/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.annotation.SuppressLint
import com.alexvt.integrity.core.IntegrityCore
import com.snatik.storage.Storage
import java.io.File

/**
 * Manages data files to be temporarily available in local storage
 */
object DataCacheFolderUtil {

    fun getSnapshotFolderPath(artifactId: Long, date: String): String {
        return getDataCacheDirectory() + File.separator + "artifact_" + artifactId +
                "_snapshot_" + date
    }

    fun createEmptyFolder(artifactId: Long, date: String): String {
        val snapshotDataDirectory = getSnapshotFolderPath(artifactId, date)
        getStorage().createDirectory(snapshotDataDirectory, true)
        return snapshotDataDirectory
    }

    fun clearFiles() {
        deleteFiles(getStorage().getFiles(getDataCacheDirectory())
                .filter { it.isFile })
    }

    fun clear(artifactId: Long, date: String) {
        deleteFiles(getStorage().getFiles(getDataCacheDirectory())
                .filter {
                    it.name.contains("artifact_" + artifactId)
                            && it.name.contains(date)
                })
    }

    fun clear(artifactId: Long) {
        deleteFiles(getStorage().getFiles(getDataCacheDirectory())
                .filter { it.name.contains("artifact_" + artifactId) })
    }

    fun clear() {
        deleteFiles(getStorage().getFiles(getDataCacheDirectory()))
    }

    fun writeTextToFile(text: String, path: String) {
        getStorage().createFile(path, text)
    }

    private fun deleteFiles(filesToDelete: List<File>) {
        for (fileToDelete in filesToDelete) {
            if (fileToDelete.isDirectory) {
                getStorage().deleteDirectory(fileToDelete.absolutePath)
            } else if (fileToDelete.isFile) {
                getStorage().deleteFile(fileToDelete.absolutePath)
            }
        }
    }

    private fun getDataCacheDirectory(): String {
        val integrityDirectory = getStorage().externalStorageDirectory + File.separator + "Integrity"
        getStorage().createDirectory(integrityDirectory)

        val dataDirectory = integrityDirectory + File.separator + "_DataCache"
        if (!getStorage().isDirectoryExists(dataDirectory)) {
            getStorage().createDirectory(dataDirectory)
        }
        return dataDirectory
    }


    @SuppressLint("StaticFieldLeak")
    private lateinit var storage: Storage // todo DI

    private fun getStorage(): Storage {
        if (!this::storage.isInitialized) {
            // todo permissions. For now they should be enabled for the installed app in App Info
            storage = Storage(IntegrityCore.context)
        }
        return storage
    }

}