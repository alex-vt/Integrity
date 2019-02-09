/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.tags

import android.content.Context
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil
import com.alexvt.integrity.lib.Tag

/**
 * Stores tags simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 */
object SimplePersistableTagRepository : TagRepository {

    private data class Tags(val tagSet: LinkedHashSet<Tag> = linkedSetOf())

    private lateinit var tags: Tags


    /**
     * Prepares database for use
     */
    override fun init(context: Context) {
        val tagsJson = PreferencesUtil.getTagsJson(context)
        if (tagsJson != null) {
            tags = JsonSerializerUtil.fromJson(tagsJson, Tags::class.java)
        }
        if (!::tags.isInitialized) {
            tags = Tags()
        }
    }

    override fun addTag(tag: Tag) {
        tags.tagSet.add(tag)
        persistAll()
    }

    override fun removeTag(name: String) {
        tags.tagSet.removeIf { it.text == name }
        persistAll()
    }

    override fun getAllTags(): List<Tag> = tags.tagSet.toList().reversed() // newest first

    override fun clear() {
        tags.tagSet.clear()
        persistAll()
    }

    /**
     * Persists tags to JSON in SharedPreferences.
     *
     * Should be called after every tag modification.
     */
    @Synchronized private fun persistAll() {
        val tagsJson = JsonSerializerUtil.toJson(tags)
        PreferencesUtil.setTagsJson(IntegrityCore.context, tagsJson)
    }
}