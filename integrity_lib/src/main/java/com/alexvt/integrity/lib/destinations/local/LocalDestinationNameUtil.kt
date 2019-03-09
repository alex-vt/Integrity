/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.destinations.local

import com.alexvt.integrity.lib.destinations.DestinationNameUtil

object LocalDestinationNameUtil : DestinationNameUtil<LocalFolderLocation> {

    override fun getFolderLocationLabel() = "Local"

    override fun getFolderLocationDescription(folderLocation: LocalFolderLocation)
            = folderLocation.folderPath
}