/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type

import android.app.Activity
import android.webkit.WebView
import com.alexvt.integrity.core.SnapshotMetadata
import com.alexvt.integrity.core.TypeMetadata
import com.alexvt.integrity.core.job.JobProgress

/**
 * Data manipulation contract for a data type
 */
interface DataTypeUtil<T: TypeMetadata> {


    /**
     * Gets data type name visible for user
     */
    fun getTypeScreenName(): String

    /**
     * Gets class of activity responsible for (starting) operations with the data type.
     *
     * Activity should accept in intent:
     * artifactId and date for viewing snapshot,
     * only artifactId for creating new snapshot for the existing artifact,
     * or neither for creating snapshot of new artifact.
     */
    fun getOperationMainActivityClass(): Class<out Activity>

    /**
     * Downloads type specific data described by given data type specific metadata.
     *
     * returns: path in data cache folder where the data is downloaded to.
     */
    suspend fun downloadData(artifactId: Long, date: String, dataTypeSpecificMetadata: T,
                             jobProgressListener: (JobProgress<SnapshotMetadata>) -> Unit): String

}