/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.metadata

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
                            val downloadSchedule: DownloadSchedule = DownloadSchedule(),
                            val archiveFolderLocations: ArrayList<FolderLocation> = arrayListOf(),
                            val tags: ArrayList<Tag> = arrayListOf(),
                            val themeColor: String = "#FFFFFF",
                            val dataTypeSpecificMetadata: TypeMetadata = EmptyTypeMetadata(),
                            val status: String = SnapshotStatus.BLUEPRINT
)

/**
 * Snapshot metadata where the data type specific part is stored as JSON string.
 *
 * Type specific part doesn't require presence of its type class during de-serialization.
 * Snapshot status can be set.
 */
data class Snapshot(val artifactId: Long = 0,
                    val date: String = "",
                    val title: String = "",
                    val description: String = "",
                    val downloadSchedule: DownloadSchedule = DownloadSchedule(),
                    val archiveFolderLocations: ArrayList<FolderLocation> = arrayListOf(),
                    val tags: ArrayList<Tag> = arrayListOf(),
                    val themeColor: String = "#FFFFFF",
                    val dataTypePackageName: String = "",
                    val dataTypeClassName: String = "",
                    val dataTypeSpecificMetadataJson: String = "",
                    val status: String = SnapshotStatus.BLUEPRINT
)

/**
 * Snapshot downloading schedule: period and conditions.
 */
data class DownloadSchedule(val periodSeconds: Long = 0, // <= 0 for single time download
                            val allowOnWifiOnly: Boolean = false,
                            val allowOnLowBattery: Boolean = false
)

/**
 * Snapshot tag, colored.
 */
data class Tag(val text: String = "",
               val color: String = "#FFFFFF"
) {
    override fun equals(other: Any?): Boolean {
        return other is Tag && other.text == text && other.color == color
    }
}

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
    val blueprintLowPriorityComparator = Comparator<Snapshot> {
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
data class MetadataCollection(val snapshots: ArrayList<Snapshot> = arrayListOf(),
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
 * General type credentials
 */
abstract class Credentials {
    abstract val title: String
}

/**
 * Credentials placeholder
 */
data class EmptyCredentials(override val title: String = ""): Credentials()

/**
 * Content type specific part of metadata, describes the corresponding data.
 */
abstract class TypeMetadata

/**
 * TypeMetadata Stub
 */
internal class EmptyTypeMetadata: TypeMetadata()
