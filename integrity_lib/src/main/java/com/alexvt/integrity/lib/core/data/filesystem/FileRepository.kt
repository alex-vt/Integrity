/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.data.filesystem

import java.io.File

interface FileRepository {

    fun getRootFolder(): String

    fun getFiles(path: String): List<File>

    fun createFolder(path: String)

    fun getText(path: String): String

    fun getBytes(path: String): ByteArray

    fun fileExists(path: String): Boolean

    fun writeFile(path: String, bytes: ByteArray)

    fun appendFile(path: String, bytes: ByteArray)

    fun renameFile(sourcePath: String, destinationPath: String)

    fun copyFolder(sourcePath: String, destinationPath: String)

    fun moveFolder(sourcePath: String, destinationPath: String)

    fun delete(path: String)
}