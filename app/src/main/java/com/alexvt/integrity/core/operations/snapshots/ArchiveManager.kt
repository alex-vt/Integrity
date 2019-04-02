/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.snapshots

import com.alexvt.integrity.lib.core.operations.filesystem.FilesystemManager
import com.alexvt.integrity.lib.core.data.metadata.SnapshotMetadata
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import javax.inject.Inject

class ArchiveManager @Inject constructor(
        private val filesystemManager: FilesystemManager
) {

    fun packSnapshot(dataCacheFolderPath: String): String {
        // todo watch job cancellation, split archive
        filesystemManager.createFolder(dataCacheFolderPath)

        val archivePath = "$dataCacheFolderPath.zip"
        ZipUtil.pack(File(dataCacheFolderPath), File(archivePath));
        return archivePath
    }

    fun unarchiveSnapshotData(localArchivePath: String, localFolderPath: String): SnapshotMetadata? {
        // todo
        return null
    }

    fun addHashToArchivePath(archivePathWithoutHash: String, hash: String): String {
        val archivePathWithHash = archivePathWithoutHash.replaceLast(".zip", "_" + hash + ".zip")
        filesystemManager.renameFile(archivePathWithoutHash, archivePathWithHash)
        return archivePathWithHash
    }

    // Replaces the last occurrence of the given regular expression regex in this char sequence
    // with specified replacement expression.
    //
    // See
    // https://github.com/arturbosch/kdit/blob/master/src/main/kotlin/io/gitlab/arturbosch/kdit/editor/util/Misc.kt
    // Similar to: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/replace-first.html
    private fun String.replaceLast(oldChars: String, newChars: String, ignoreCase: Boolean = false): String {
        val index = lastIndexOf(oldChars, ignoreCase = ignoreCase)
        return if (index < 0) this else this.replaceRange(index, index + oldChars.length, newChars)
    }


}