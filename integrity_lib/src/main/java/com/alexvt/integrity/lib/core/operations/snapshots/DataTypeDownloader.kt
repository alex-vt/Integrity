/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.snapshots

import com.alexvt.integrity.lib.core.data.metadata.TypeMetadata

interface DataTypeDownloader<T: TypeMetadata> {

    /**
     * Gets generic parameter class of a derived class.
     * A workaround needed because of JVM generic type erasure. // todo look for elegant solution
     */
    fun getTypeMetadataClass(): Class<T>

    /**
     * Downloads type specific data described by given data type specific metadata.
     */
    fun downloadData(dataFolderName: String, artifactId: Long, date: String,
                     typeMetadata: T): String

    /**
     * Generates snapshot preview image using downloaded snapshot data.
     */
    fun generateOfflinePreview(dataFolderName: String, artifactId: Long, date: String,
                               typeMetadata: T)
}