/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import com.alexvt.integrity.lib.android.operations.snapshots.DataTypeService

class GitHubTypeService: DataTypeService<GitHubTypeMetadata>() {

    override fun getTypeMetadataClass() = GitHubTypeMetadata::class.java


    override fun downloadData(dataFolderName: String, artifactId: Long, date: String,
                              typeMetadata: GitHubTypeMetadata): String {
        val snapshotPath = dataFolderManager.getSnapshotFolderPath(dataFolderName,
                artifactId, date)

        // todo implement

        return snapshotPath
    }

    /**
     * Gets saved user page screenshot.
     */
    override fun generateOfflinePreview(dataFolderName: String, artifactId: Long, date: String,
                                        typeMetadata: GitHubTypeMetadata) {
        // todo implement
    }

}