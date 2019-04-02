/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.data.destinations

import com.alexvt.integrity.lib.core.data.destinations.DestinationNameRepository
import com.alexvt.integrity.lib.core.data.destinations.LocalFolderLocation
import com.alexvt.integrity.lib.core.data.destinations.SambaFolderLocation
import com.alexvt.integrity.lib.core.data.metadata.FolderLocation

class AndroidDestinationNameRepository : DestinationNameRepository {

    private val folderLocationNameMap = linkedMapOf(
            LocalFolderLocation::class.java to "Folder on device",
            SambaFolderLocation::class.java to "LAN (Samba) folder"
    )

    override fun getDestinationName(destinationClass: Class<out FolderLocation>)
            = folderLocationNameMap[destinationClass]!!

}