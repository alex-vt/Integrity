/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import com.alexvt.integrity.core.util.Initializable
import com.alexvt.integrity.lib.metadata.FolderLocation
import com.alexvt.integrity.lib.metadata.Tag

/**
 * Manager of repository of app settings (singleton).
 */
interface SettingsRepository : Initializable {

    /**
     * Registers database contents changes listener with a tag.
     * todo narrow down to tracking changes of subset of data
     */
    fun addChangesListener(tag: String, changesListener: (IntegrityAppSettings) -> Unit)

    /**
     * Removes database contents changes listener by a tag
     */
    fun removeChangesListener(tag: String)

    fun set(integrityAppSettings: IntegrityAppSettings)

    fun get(): IntegrityAppSettings

    /**
     * Rewrites settings with the default ones.
     */
    fun resetToDefault()

    fun addTag(tag: Tag)

    fun removeTag(name: String)

    fun getAllTags(): List<Tag>

    fun clearTags()


    fun addFolderLocation(folderLocation: FolderLocation): String

    fun getAllFolderLocations(): List<FolderLocation>

    fun removeFolderLocation(title: String)

    fun clearFolderLocations()
}