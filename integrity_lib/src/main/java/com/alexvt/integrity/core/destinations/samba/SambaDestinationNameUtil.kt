/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations.samba

import com.alexvt.integrity.core.destinations.DestinationNameUtil


object SambaDestinationNameUtil : DestinationNameUtil<SambaFolderLocation> {

    override fun getFolderLocationLabel() = "Samba"

    override fun getFolderLocationDescription(folderLocation: SambaFolderLocation)
            = folderLocation.fullPath
}