/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import android.content.Context
import com.alexvt.integrity.core.util.Initializable
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.Tag

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

    fun set(context: Context, integrityAppSettings: IntegrityAppSettings)

    fun get(): IntegrityAppSettings

    /**
     * Rewrites settings with the default ones.
     */
    fun resetToDefault(context: Context)

    fun addTag(context: Context, tag: Tag)

    fun removeTag(context: Context, name: String)

    fun getAllTags(): List<Tag>

    fun clearTags(context: Context)


    fun addFolderLocation(context: Context, folderLocation: FolderLocation): String

    fun getAllFolderLocations(): List<FolderLocation>

    fun removeFolderLocation(context: Context, title: String)

    fun clearFolderLocations(context: Context)
}