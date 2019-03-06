/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations.samba

import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.Credentials

/**
 * A location in Samba filesystem, defined by path, user name and password
 */
data class SambaFolderLocation(
        override val title: String = "",
        val fullPath: String = ""
): FolderLocation() {
    override fun equals(other: Any?): Boolean {
        return other is SambaFolderLocation
                && other.title == title
                && other.fullPath == fullPath
    }
}

data class SambaFolderLocationCredentials(
        override val title: String = "",
        val user: String = "",
        val password: String = ""
): Credentials()
