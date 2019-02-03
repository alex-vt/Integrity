/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import com.snatik.storage.Storage
import java.io.File

/**
 * Manages data files to be temporarily available in local storage
 */
object DataCacheFolderUtil {

    fun getSnapshotFolderPath(context: Context, artifactId: Long, date: String): String {
        return getDataCacheDirectory(context) + File.separator + "artifact_" + artifactId +
                "_snapshot_" + date
    }

    fun getSnapshotFileSimpleNames(context: Context, artifactId: Long, date: String): Set<String> {
        return getStorage(context).getFiles(getSnapshotFolderPath(context, artifactId, date))
                .filter { it.isFile }
                .map { it.nameWithoutExtension }
                .toSet()
    }

    fun ensureSnapshotFolder(context: Context, artifactId: Long, date: String): String {
        val snapshotDataDirectory = getSnapshotFolderPath(context, artifactId, date)
        getStorage(context).createDirectory(snapshotDataDirectory, false)
        return snapshotDataDirectory
    }

    fun clearFiles(context: Context) {
        deleteFiles(context, getStorage(context).getFiles(getDataCacheDirectory(context))
                .filter { it.isFile })
    }

    fun clear(context: Context, artifactId: Long, date: String) {
        deleteFiles(context, getStorage(context).getFiles(getDataCacheDirectory(context))
                .filter {
                    it.name.contains("artifact_" + artifactId)
                            && it.name.contains(date)
                })
    }

    fun clear(context: Context, artifactId: Long) {
        deleteFiles(context, getStorage(context).getFiles(getDataCacheDirectory(context))
                .filter { it.name.contains("artifact_" + artifactId) })
    }

    fun clear(context: Context) {
        deleteFiles(context, getStorage(context).getFiles(getDataCacheDirectory(context)))
    }

    fun writeTextToFile(context: Context, text: String, path: String) {
        getStorage(context).createFile(path, text)
    }

    fun writeImageToFile(context: Context, image: Bitmap, path: String) {
        getStorage(context).createFile(path, image)
    }

    fun addTextToFile(context: Context, text: String, path: String) {
        if (!fileExists(context, path)) {
            getStorage(context).createFile(path, "")
        }
        getStorage(context).appendFile(path, text)
    }

    fun fileExists(context: Context, path: String) = getStorage(context).isFileExist(path)

    fun readTextFromFile(context: Context, path: String) = if (fileExists(context, path)) {
        getStorage(context).readTextFile(path)?: ""
    } else {
        ""
    }

    private fun deleteFiles(context: Context, filesToDelete: List<File>) {
        for (fileToDelete in filesToDelete) {
            if (fileToDelete.isDirectory) {
                getStorage(context).deleteDirectory(fileToDelete.absolutePath)
            } else if (fileToDelete.isFile) {
                getStorage(context).deleteFile(fileToDelete.absolutePath)
            }
        }
    }

    private fun getDataCacheDirectory(context: Context): String {
        val integrityDirectory = getStorage(context).externalStorageDirectory + File.separator + "Integrity"
        getStorage(context).createDirectory(integrityDirectory)

        val dataDirectory = integrityDirectory + File.separator + "_DataCache"
        if (!getStorage(context).isDirectoryExists(dataDirectory)) {
            getStorage(context).createDirectory(dataDirectory)
        }
        return dataDirectory
    }


    @SuppressLint("StaticFieldLeak")
    private lateinit var storage: Storage // todo DI

    private fun getStorage(context: Context): Storage {
        if (!this::storage.isInitialized) {
            // todo permissions. For now they should be enabled for the installed app in App Info
            storage = Storage(context)
        }
        return storage
    }

}