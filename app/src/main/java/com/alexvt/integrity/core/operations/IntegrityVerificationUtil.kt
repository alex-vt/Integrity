/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations

import com.alexvt.integrity.lib.metadata.SnapshotMetadata

object IntegrityVerificationUtil {

    /**
     * Checks integrity of all stored and archived metadata, and also optionally the data.
     *
     * If data verification is not required,
     * hash of metadata in database is calculated and compared to given verified value,
     * and presence of correctly named data archives is checked according to metadata.
     *
     * If data verification is required,
     * then in addition to the previous steps
     * hash of every data archive is calculated and compared to the
     * value in snapshot metadata in database.
     */
    fun verify(snapshots: List<SnapshotMetadata>, alsoVerifyData: Boolean): Boolean {
        // todo
        return false
    }

}