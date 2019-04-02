/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.data.types.blog
/**
 * Holds Blog Type metadata and properties of environment for its data downloading.
 *
 * Serves for downloading algorithm code clarity.
 */
data class BlogMetadataDownload(
        val dataFolderName: String,
        val artifactId: Long,
        val date: String,
        val metadata: BlogTypeMetadata,
        val snapshotPath: String
)