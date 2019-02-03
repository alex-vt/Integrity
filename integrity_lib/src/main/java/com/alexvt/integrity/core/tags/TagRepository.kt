/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.tags

import android.content.Context
import com.alexvt.integrity.lib.Tag

/**
 * Manager of repository of tags assigned to snapshots.
 */
interface TagRepository {
    /**
     * Prepares database for use
     */
    fun init(context: Context)

    /**
     * Adds a unique (by name) tag
     */
    fun addTag(tag: Tag)

    /**
     * Removes tag by name
     */
    fun removeTag(name: String)

    /**
     * Gets all unique tags
     */
    fun getAllTags(): List<Tag>

    /**
     * Deletes all tags from database
     */
    fun clear()
}