/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import com.alexvt.integrity.core.type.blog.BlogTypeMetadata

/**
 * Metadata of snapshot (at a given date-time) of data from given source (artifact) valuable for user.
 * Is unique by artifactId and date.
 *
 * Title, description, data archive path and content type specific part of metadata can be edited
 * within the same artifact ID.
 *
 * Data itself is stored separately in an archive and can be accessed by the archive path.
 */
data class SnapshotMetadata(val artifactId: Long = 0,
                            val date: String = "",
                            val title: String = "",
                            val description: String = "",
                            val archiveFolderLocations: ArrayList<FolderLocation> = arrayListOf(),
                            val dataTypeSpecificMetadata: TypeMetadata = BlogTypeMetadata(),
                            val blueprint: Boolean = true
)

/**
 * A collection of metadata snapshots,
 * with hash of that metadata for integrity control.
 *
 * In the particular case, collection of all metadata snapshots of artifact
 * comprises that artifact itself.
 */
data class MetadataCollection(val snapshotMetadataList: ArrayList<SnapshotMetadata> = arrayListOf(),
                              val metadataHash: String = "")

// todo add operation status and/or callbacks for the future async operations

/**
 * Archive location is defined in different ways
 * depending on if a local or some kind of remote filesystem is used.
 */
abstract class FolderLocation

/**
 * Content type specific part of metadata, describes the corresponding data.
 */
abstract class TypeMetadata
