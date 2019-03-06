/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations.local

import android.content.ComponentName
import com.alexvt.integrity.core.destinations.ArchiveLocationUtil

object LocalLocationUtil : ArchiveLocationUtil<LocalFolderLocation> {

    override fun getFolderLocationLabel() = "Local"

    override fun getFolderLocationDescription(folderLocation: LocalFolderLocation)
            = folderLocation.folderPath

    override fun getViewMainActivityComponent() = ComponentName("com.alexvt.integrity",
            "com.alexvt.integrity.ui.destinations.local.LocalLocationActivity")
}