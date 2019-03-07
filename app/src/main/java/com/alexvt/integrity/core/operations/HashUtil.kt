/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations

import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.MetadataCollection

object HashUtil {

    fun updateHash(metadataCollection: MetadataCollection): MetadataCollection {
        return metadataCollection.copy(metadataHash = getHash(metadataCollection.snapshots))
    }

    fun getHash(snapshotList: List<Snapshot>): String {
        // todo compute
        return snapshotList.hashCode().toString(16)
    }

    fun getFileHash(localFilePath: String): String {
        // todo compute
        return localFilePath.hashCode().toString(16).replace("-", "")
    }

}