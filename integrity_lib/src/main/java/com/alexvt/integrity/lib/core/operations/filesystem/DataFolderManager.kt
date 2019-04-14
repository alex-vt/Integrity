/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.filesystem

import com.alexvt.integrity.lib.core.data.filesystem.FileRepository
import java.io.File

/**
 * Manages data files to be temporarily available in local storage
 */
class DataFolderManager(private val fileRepository: FileRepository) {

    fun getSnapshotPreviewPath(dataFolderName: String, artifactId: Long, date: String)
            = getSnapshotFolderPath(dataFolderName, artifactId, date) + "/_preview.png"

    fun getSnapshotDataChunksPath(dataFolderName: String, artifactId: Long, date: String)
            = getSnapshotFolderPath(dataFolderName, artifactId, date) + "/text.txt"

    fun getSnapshotFolderPath(dataFolderName: String, artifactId: Long,
                              date: String): String {
        return getDataCacheDirectory(dataFolderName) + File.separator +
                "artifact_" + artifactId + "_snapshot_" + date
    }

    fun getSnapshotFileSimpleNames(dataFolderName: String, artifactId: Long,
                                   date: String): Set<String> {
        return fileRepository.getFiles(getSnapshotFolderPath(dataFolderName,
                artifactId, date))
                .filter { it.isFile }
                .map { it.nameWithoutExtension }
                .toSet()
    }

    fun ensureSnapshotFolder(dataFolderName: String, artifactId: Long,
                             date: String): String {
        val snapshotDataDirectory = getSnapshotFolderPath(dataFolderName, artifactId, date)
        fileRepository.createFolder(snapshotDataDirectory)
        return snapshotDataDirectory
    }

    fun clearFiles(dataFolderName: String) {
        deleteFiles(fileRepository.getFiles(getDataCacheDirectory(dataFolderName))
                .filter { it.isFile })
    }

    fun clear(dataFolderName: String, artifactId: Long, date: String) {
        deleteFiles(fileRepository.getFiles(getDataCacheDirectory(dataFolderName))
                .filter {
                    it.name.contains("artifact_" + artifactId)
                            && it.name.contains(date)
                })
    }

    fun clear(dataFolderName: String, artifactId: Long) {
        deleteFiles(fileRepository.getFiles(getDataCacheDirectory(dataFolderName))
                .filter { it.name.contains("artifact_" + artifactId) })
    }

    fun clear(dataFolderName: String) {
        deleteFiles(fileRepository.getFiles(getDataCacheDirectory(dataFolderName)))
    }

    fun deleteFolder(dataFolderName: String) {
        fileRepository.delete(getDataCacheDirectory(dataFolderName))
    }

    fun writeTextToFile(text: String, path: String) {
        fileRepository.writeFile(path, text.toByteArray())
    }

    fun writeFile(bytes: ByteArray, path: String) {
        fileRepository.writeFile(path, bytes)
    }

    fun addTextToFile(text: String, path: String) {
        if (!fileExists(path)) {
            fileRepository.writeFile(path, "".toByteArray())
        }
        fileRepository.appendFile(path, text.toByteArray())
    }

    fun fileExists(path: String) = fileRepository.fileExists(path)

    fun readTextFromFile(path: String) = fileRepository.getText(path)

    fun moveDataCacheFolder(sourceFolderName: String,
                            destinationFolderName: String) {
        val sourcePath = fileRepository.getRootFolder() + File.separator + sourceFolderName
        val destinationPath = fileRepository.getRootFolder() + File.separator + destinationFolderName
        fileRepository.moveFolder(sourcePath, destinationPath)
    }

    private fun deleteFiles(filesToDelete: List<File>) {
        for (fileToDelete in filesToDelete) {
            fileRepository.delete(fileToDelete.absolutePath)
        }
    }

    private fun getDataCacheDirectory(dataFolderName: String): String {
        val integrityDirectory = fileRepository.getRootFolder() + File.separator + dataFolderName
        fileRepository.createFolder(integrityDirectory)

        val dataDirectory = integrityDirectory + File.separator + "_DataCache"
        fileRepository.createFolder(dataDirectory)
        return dataDirectory
    }

}