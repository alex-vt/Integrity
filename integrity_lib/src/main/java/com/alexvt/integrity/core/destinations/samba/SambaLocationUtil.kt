/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations.samba

import android.content.ComponentName
import com.alexvt.integrity.core.destinations.ArchiveLocationUtil


object SambaLocationUtil : ArchiveLocationUtil<SambaFolderLocation> {

    override fun getFolderLocationLabel() = "Samba"

    override fun getFolderLocationDescription(folderLocation: SambaFolderLocation)
            = folderLocation.fullPath

    override fun getViewMainActivityComponent() = ComponentName("com.alexvt.integrity",
            "com.alexvt.integrity.ui.destinations.samba.SambaLocationActivity")
}