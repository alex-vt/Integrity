/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.util

import android.content.Context
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.metadata.SnapshotMetadata
import com.alexvt.integrity.lib.core.data.metadata.TypeMetadata
import com.alexvt.integrity.lib.core.util.JsonSerializerUtil

object TypeSpecificMetadataConverter {

    fun toTypeSpecificMetadata(snapshot: Snapshot) = SnapshotMetadata(
            artifactId = snapshot.artifactId,
            title = snapshot.title,
            date = snapshot.date,
            description = snapshot.description,
            downloadSchedule = snapshot.downloadSchedule,
            archiveFolderLocations = snapshot.archiveFolderLocations,
            tags = snapshot.tags,
            themeColor = snapshot.themeColor,
            dataTypeSpecificMetadata = JsonSerializerUtil.fromJson(
                    snapshot.dataTypeSpecificMetadataJson,
                    Class.forName(snapshot.dataTypeClassName) as Class<TypeMetadata>),
            status = snapshot.status
    )

    fun fromTypeSpecificMetadata(context: Context, snapshotMetadata: SnapshotMetadata) = Snapshot(
            artifactId = snapshotMetadata.artifactId,
            title = snapshotMetadata.title,
            date = snapshotMetadata.date,
            description = snapshotMetadata.description,
            downloadSchedule = snapshotMetadata.downloadSchedule,
            archiveFolderLocations = snapshotMetadata.archiveFolderLocations,
            tags = snapshotMetadata.tags,
            themeColor = snapshotMetadata.themeColor,
            dataTypeClassName = snapshotMetadata.dataTypeSpecificMetadata.javaClass.name,
            dataTypePackageName = context.packageName,
            dataTypeSpecificMetadataJson = JsonSerializerUtil.toJson(
                    snapshotMetadata.dataTypeSpecificMetadata),
            status = snapshotMetadata.status
    )

}