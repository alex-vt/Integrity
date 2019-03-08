/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.destinations.local

import com.alexvt.integrity.lib.metadata.FolderLocation

/**
 * A path in local filesystem, defined simply by a single string.
 */
data class LocalFolderLocation(
        override val title: String = "",
        val folderPath: String = ""
) : FolderLocation() {
    override fun equals(other: Any?): Boolean {
        return other is LocalFolderLocation
                && other.title == title
                && other.folderPath == folderPath
    }
}
