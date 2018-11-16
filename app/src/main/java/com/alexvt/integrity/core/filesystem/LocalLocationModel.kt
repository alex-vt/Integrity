/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import com.alexvt.integrity.core.FolderLocation

/**
 * A path in local filesystem, defined simply by a single string.
 */
data class LocalFolderLocation(val folderPath: String): FolderLocation() {
    constructor() : this("")

    override fun hashCode(): Int {
        return LocalFolderLocation::class.java.name.hashCode() + folderPath.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is LocalFolderLocation) {
            other.hashCode() == hashCode()
        } else {
            false
        }
    }
}

// todo add SMB, Dropbox. Those will have credentials.

