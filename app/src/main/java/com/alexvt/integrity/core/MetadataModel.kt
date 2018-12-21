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
                            val status: String = SnapshotStatus.BLUEPRINT
)

/**
 * Snapshot downloading status.
 *
 * Lifecycle:
 * BLUEPRINT -+-> IN_PROGRESS -+-> COMPLETE
 *  create    | download  stop |    done
 *            +<- INCOMPLETE <-+
 */
object SnapshotStatus {
    val BLUEPRINT = "blueprint" // no corresponding data downloaded
    val IN_PROGRESS = "in_progress" // downloading data now
    val INCOMPLETE = "incomplete" // data partially downloaded, then interrupted
    val COMPLETE = "complete" // data downloaded
}

object SnapshotCompareUtil {
    // COMPLETE = INCOMPLETE = IN_PROGRESS > BLUEPRINT
    private val blueprintLowPriorityOrderMap = mapOf(
            Pair(SnapshotStatus.COMPLETE, 0),
            Pair(SnapshotStatus.INCOMPLETE, 0),
            Pair(SnapshotStatus.IN_PROGRESS, 0),
            Pair(SnapshotStatus.BLUEPRINT, -1)
    )
    val blueprintLowPriorityComparator = Comparator<SnapshotMetadata> {
        first, second -> blueprintLowPriorityOrderMap[second.status]!!
        - blueprintLowPriorityOrderMap[first.status]!!
    }
}


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
abstract class FolderLocation {
    abstract val title: String
}

/**
 * Archive location credentials are stored separately from location, but with the same title.
 * Not stored in data archives.
 */
abstract class FolderLocationCredentials {
    abstract val title: String
}

/**
 * Credentials placeholder
 */
data class EmptyLocationCredentials(override val title: String = ""): FolderLocationCredentials()

/**
 * Content type specific part of metadata, describes the corresponding data.
 */
abstract class TypeMetadata
