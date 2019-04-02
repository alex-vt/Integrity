/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.operations.filesystem

import android.content.Context
import com.alexvt.integrity.lib.core.operations.filesystem.FilesystemManager
import com.snatik.storage.Storage
import org.zeroturnaround.zip.commons.FileUtils
import java.io.File

class AndroidFilesystemManager(private val context: Context) : FilesystemManager {

    private val storage by lazy { Storage(context) }

    override fun getRootFolder(): String = storage.externalStorageDirectory

    override fun getFiles(path: String): List<File> = storage.getFiles(path)

    override fun createFolder(path: String) {
        storage.createDirectory(path, false)
    }

    override fun getText(path: String) = if (fileExists(path)) {
        storage.readTextFile(path) ?: ""
    } else {
        ""
    }

    override fun getBytes(path: String): ByteArray = storage.readFile(path)

    override fun fileExists(path: String) = storage.isFileExist(path)

    override fun writeFile(path: String, bytes: ByteArray) {
        storage.createFile(path, bytes)
    }

    override fun appendFile(path: String, bytes: ByteArray) = storage.appendFile(path, bytes)

    override fun renameFile(sourcePath: String, destinationPath: String) {
        storage.rename(sourcePath, destinationPath)
    }

    override fun copyFolder(sourcePath: String, destinationPath: String) {
        storage.copy(sourcePath, destinationPath)
    }

    override fun moveFolder(sourcePath: String, destinationPath: String) {
        FileUtils.moveDirectory(File(sourcePath), File(destinationPath)) // todo replace dependency
    }

    override fun delete(path: String) {
        if (fileExists(path)) storage.deleteFile(path) else storage.deleteDirectory(path)
    }
}