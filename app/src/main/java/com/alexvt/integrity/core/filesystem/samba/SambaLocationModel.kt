/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem.samba

import com.alexvt.integrity.core.FolderLocation

/**
 * A location in Samba filesystem, defined by path, user name and password
 */
data class SambaFolderLocation(
        val user: String,
        val password: String,
        val fullPath: String
): FolderLocation() {
    constructor() : this("", "", "")

    override fun hashCode(): Int {
        return SambaFolderLocation::class.java.name.hashCode() + fullPath.hashCode() + user.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SambaFolderLocation) {
            other.hashCode() == hashCode()
        } else {
            false
        }
    }

    override fun toString(): String {
        return fullPath
    }
}
