/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations

import com.alexvt.integrity.lib.destinations.local.LocalFolderLocation
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocation
import com.alexvt.integrity.lib.metadata.FolderLocation

object DestinationUtilResolver {

    fun getDestinationClasses() = listOf(
            LocalFolderLocation::class.java,
            SambaFolderLocation::class.java
    )

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> get(dataArchiveLocationClass: Class<F>): DestinationUtil<F> {
        val utilClassName = dataArchiveLocationClass.name
                .replace("FolderLocation", "DestinationUtil")
                .replace(".integrity.lib.destinations.", ".integrity.core.destinations.")
        val destinationUtil = Class.forName(utilClassName).newInstance() as DestinationUtil<F>
        android.util.Log.v("DestinationUtilResolver", "Resolved: $destinationUtil")
        return destinationUtil
    }

}