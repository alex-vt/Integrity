/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.destinations

import android.content.ComponentName
import com.alexvt.integrity.lib.FolderLocation

/**
 * Data archive naming for a type of folder location
 */
interface ArchiveLocationUtil<F: FolderLocation> {

    fun getFolderLocationLabel(): String

    fun getFolderLocationDescription(folderLocation: F): String

    /**
     * Gets component name of activity responsible for (starting) viewing or editing
     * this folder location.
     *
     * Activity should accept in intent:
     * title of file location to view/edit.
     */
    fun getViewMainActivityComponent(): ComponentName

}